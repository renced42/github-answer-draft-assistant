package hu.gov.nav.answerdraft;

import java.util.*;

final class KnowledgeRepositoryClient {
    private static final String PLACEHOLDER="[IDE ÍRD AZ ELLENŐRZÖTT VÉGLEGES VÁLASZT]";
    private static final Set<String> BLOCKING_LABELS=Set.of("rejected","outdated","needs-correction");
    private static final Set<String> ALLOWED_ORIGINS=Set.of("official-source","expert-confirmed");
    private static final Set<String> ALLOWED_DOCUMENTATION_STATES=Set.of("documented","documentation-gap");
    private final WebClient http;
    private final Config config;
    private final Map<String,String> headers;
    private final List<String> warnings=new ArrayList<>();

    KnowledgeRepositoryClient(WebClient http,Config config){
        this.http=http;this.config=config;
        this.headers=config.knowledgeToken().isBlank()?Map.of():Map.of("Accept","application/vnd.github+json","Authorization","Bearer "+config.knowledgeToken(),"X-GitHub-Api-Version","2022-11-28");
    }

    boolean enabled(){return !config.knowledgeRepository().isBlank();}

    List<Source> collectApproved(Question question){
        if(!enabled())return List.of();
        String url="https://api.github.com/repos/"+config.knowledgeRepository()+"/issues?state=all&labels="+WebClient.query(config.knowledgeApprovedLabel())+"&sort=updated&direction=desc&per_page=100";
        try{
            List<Candidate> candidates=new ArrayList<>();String system=SystemClassifier.classify(question);Set<String> keywords=new HashSet<>(KeywordExtractor.extract(question));
            for(Object raw:Json.array(Json.parse(http.get(url,headers).body()))){
                Map<String,Object> issue=Json.object(raw);if(issue.containsKey("pull_request"))continue;
                if(hasBlockingLabel(issue))continue;
                String title=Json.string(issue.get("title")),body=Json.string(issue.get("body"));
                String storedSystem=namedSection(body,"Azonosított rendszer");
                if(!sameSystem(system,storedSystem,title+"\n"+body))continue;
                String answer=namedSection(body,"Ellenőrzött végleges válasz");
                if(missingAnswer(answer))answer=approvalCommentAnswer(loadComments(((Number)issue.get("number")).intValue()));
                if(missingAnswer(answer)){warn("A jóváhagyott tudás-issue nem tartalmaz kitöltött végleges választ vagy /approve hozzászólást, ezért kimaradt: "+Json.string(issue.get("html_url")));continue;}
                Set<String> labels=labels(issue);
                String origin=controlledValue(namedSection(body,"Tudás eredete"),labels,ALLOWED_ORIGINS);
                String documentation=controlledValue(namedSection(body,"Nyilvános dokumentáció állapota"),labels,ALLOWED_DOCUMENTATION_STATES);
                if(origin.isBlank()||documentation.isBlank()){warn("A jóváhagyott tudás-issue eredete vagy dokumentációs állapota nincs szabályosan megadva, ezért kimaradt: "+Json.string(issue.get("html_url")));continue;}
                String sources=namedSection(body,"Felhasznált források");int score=relevance(title+" "+body,keywords);
                String content="Azonosított rendszer: "+system+"\nTudás eredete: "+origin+"\nNyilvános dokumentáció állapota: "+documentation+"\n\nELLENŐRZÖTT VÁLASZ:\n"+answer+(sources.isBlank()?"":"\n\nNYILVÁNOSAN HIVATKOZHATÓ FORRÁSOK:\n"+sources);
                content=sanitizePrivateKnowledgeLinks(content,config.knowledgeRepository());
                candidates.add(new Candidate(new Source("Jóváhagyott belső tudás","",WebClient.shorten(content,7000),160+score,SourceType.APPROVED_KNOWLEDGE),score));
            }
            return candidates.stream().sorted(Comparator.comparingInt(Candidate::score).reversed()).limit(config.knowledgeLimit()).map(Candidate::source).toList();
        }catch(Exception error){warn("A privát tudástár olvasása sikertelen: "+error.getMessage());return List.of();}
    }

    String createReviewIssue(Question question,String draft,List<Source> sources,List<String> generationWarnings){
        if(!enabled()||!config.createReviewIssue()||config.dryRun())return "";
        String existing=findExistingReview(question.url());if(!existing.isBlank()){System.out.println("A kérdéshez már létezik privát review issue: "+existing);return existing;}
        String system=SystemClassifier.classify(question);String title=WebClient.shorten("[DRAFT] "+system+" – "+question.title(),240);
        String body=reviewBody(question,system,draft,sources,generationWarnings,config.knowledgeApprovedLabel());
        Map<String,Object> request=new LinkedHashMap<>();request.put("title",title);request.put("body",body);request.put("labels",List.of(config.knowledgeCandidateLabel()));
        try{
            Map<String,Object> issue=Json.object(Json.parse(http.postJson("https://api.github.com/repos/"+config.knowledgeRepository()+"/issues",request,headers).body()));
            String url=Json.string(issue.get("html_url"));if(url.isBlank())throw new IllegalStateException("A GitHub nem adott review issue URL-t.");return url;
        }catch(Exception error){warn("A privát review issue létrehozása sikertelen: "+error.getMessage());return "";}
    }

    List<String> warnings(){return List.copyOf(warnings);}

    static String reviewBody(Question question,String system,String draft,List<Source> sources,List<String> warnings,String approvedLabel){
        StringBuilder sourceList=new StringBuilder();for(Source source:sources){if(source.privateKnowledge()||source.url().isBlank())sourceList.append("- ").append(source.title()).append(" (belső, ügyfélnek nem hivatkozható)\n");else sourceList.append("- [").append(source.title()).append("](").append(source.url()).append(")\n");}
        String warningText=warnings.isEmpty()?"Nincs.":String.join("\n",warnings);
        return """
                > **Állapot:** AI által készített tervezet, emberi ellenőrzés szükséges. A `%s` címke csak a végleges válasz kitöltése után adható hozzá.

                ## Eredeti kérdés

                **Repository:** %s
                **Típus/sorszám:** %s #%d
                **Szerző:** @%s
                **Eredeti URL:** %s

                ### Kérdés címe

                %s

                ### Kérdés szövege

                %s

                ## Azonosított rendszer

                %s

                ## AI által készített tervezet

                %s

                ## Ellenőrzött végleges válasz

                Az issue törzsét nem kell szerkesztened. Az oldal alján, az **Add a comment** mezőbe írd:

                ```text
                /approve
                Ide írd vagy másold az ügyfélnek adható, ellenőrzött végleges választ.
                ```

                ## Tudás eredete

                A jobb oldali **Labels** választóban adj hozzá pontosan egyet: `official-source` vagy `expert-confirmed`.

                ## Nyilvános dokumentáció állapota

                A jobb oldali **Labels** választóban adj hozzá pontosan egyet: `documented` vagy `documentation-gap`.

                ## Felhasznált források

                %s
                ## Talált problémák

                - [ ] Más NAV-rendszerrel keverte össze
                - [ ] Nem igazolt állítást tartalmazott
                - [ ] Nem vizsgálta meg a releváns XSD-t vagy specifikációt
                - [ ] Elavult forrást használt
                - [ ] Hibás vagy nem közvetlen hivatkozást adott

                ## Futási figyelmeztetések

                %s

                ## Jóváhagyási nyilatkozat

                A `%s` címke hozzáadása azt jelenti, hogy az **Ellenőrzött végleges válasz**, annak eredete, dokumentációs állapota és forrásai a későbbi kérdéseknél átadhatók a konfigurált külső AI-szolgáltatónak. A privát issue URL-je nem kerül átadásra.
                """.formatted(approvedLabel,question.repository(),question.kind(),question.number(),question.author(),question.url(),question.title(),question.body(),system,draft,sourceList,warningText,approvedLabel);
    }

    static String section(String body,String startHeading,String nextHeading){
        if(body==null)return "";int start=body.indexOf(startHeading);if(start<0)return "";start+=startHeading.length();int end;
        if("## ".equals(nextHeading)){end=body.indexOf("\n## ",start);}else end=body.indexOf(nextHeading,start);
        if(end<0)end=body.length();return body.substring(start,end).trim();
    }

    static String namedSection(String body,String title){
        if(body==null||body.isBlank())return "";
        java.util.regex.Pattern heading=java.util.regex.Pattern.compile("(?m)^#{2,6}\\s+"+java.util.regex.Pattern.quote(title)+"\\s*$",java.util.regex.Pattern.CASE_INSENSITIVE|java.util.regex.Pattern.UNICODE_CASE);
        java.util.regex.Matcher matcher=heading.matcher(body);if(!matcher.find())return "";int start=matcher.end();
        java.util.regex.Matcher next=java.util.regex.Pattern.compile("(?m)^#{2,6}\\s+.+$").matcher(body);next.region(start,body.length());int end=next.find()?next.start():body.length();return body.substring(start,end).trim();
    }

    static String sanitizePrivateKnowledgeLinks(String text,String repository){
        if(text==null||text.isBlank()||repository==null||repository.isBlank())return text==null?"":text;
        String prefix="https://github.com/"+repository+"/issues/";
        return text.replaceAll(java.util.regex.Pattern.quote(prefix)+"\\d+(?:[#?][^\\s)]*)?","[BELSŐ LINK ELTÁVOLÍTVA]");
    }

    static String approvalCommentAnswer(List<Object> comments){
        for(int index=comments.size()-1;index>=0;index--){
            String body=Json.string(Json.object(comments.get(index)).get("body")).trim();
            if(!(body.equals("/approve")||body.startsWith("/approve\n")||body.startsWith("/approve\r\n")))continue;
            String answer=body.substring("/approve".length()).stripLeading();
            if(!answer.isBlank())return answer;
        }
        return "";
    }

    private List<Object> loadComments(int number){
        try{return Json.array(Json.parse(http.get("https://api.github.com/repos/"+config.knowledgeRepository()+"/issues/"+number+"/comments?per_page=100",headers).body()));}
        catch(Exception error){warn("A tudás-issue hozzászólásainak olvasása sikertelen (#"+number+"): "+error.getMessage());return List.of();}
    }

    private static boolean missingAnswer(String answer){return answer==null||answer.isBlank()||answer.contains(PLACEHOLDER)||answer.startsWith("Az issue törzsét nem kell szerkesztened.");}

    private boolean sameSystem(String expected,String stored,String fallbackText){
        if("NEM AZONOSÍTOTT".equals(expected))return true;
        if(!stored.isBlank())return expected.equalsIgnoreCase(stored.trim());
        return SystemClassifier.sameSystem(expected,fallbackText);
    }

    private int relevance(String text,Set<String> keywords){String value=text.toLowerCase(Locale.ROOT);int score=0;for(String keyword:keywords)if(value.contains(keyword.toLowerCase(Locale.ROOT)))score+=10;return score;}
    private boolean hasBlockingLabel(Map<String,Object> issue){for(Object raw:Json.array(issue.get("labels"))){String name=Json.string(Json.object(raw).get("name")).toLowerCase(Locale.ROOT);if(BLOCKING_LABELS.contains(name))return true;}return false;}
    private Set<String> labels(Map<String,Object> issue){Set<String> result=new HashSet<>();for(Object raw:Json.array(issue.get("labels")))result.add(Json.string(Json.object(raw).get("name")).trim().toLowerCase(Locale.ROOT));return result;}
    private String controlledValue(String section,Set<String> labels,Set<String> allowed){String normalized=section.trim().toLowerCase(Locale.ROOT);for(String value:allowed)if(normalized.equals(value)||labels.contains(value))return value;return "";}
    private String findExistingReview(String questionUrl){
        try{String url="https://api.github.com/repos/"+config.knowledgeRepository()+"/issues?state=all&sort=created&direction=desc&per_page=100";for(Object raw:Json.array(Json.parse(http.get(url,headers).body()))){Map<String,Object> issue=Json.object(raw);if(issue.containsKey("pull_request"))continue;if(Json.string(issue.get("body")).contains(questionUrl))return Json.string(issue.get("html_url"));}}
        catch(Exception error){warn("A meglévő review issue ellenőrzése sikertelen: "+error.getMessage());}
        return "";
    }
    private void warn(String message){warnings.add(message);System.out.println("Figyelmeztetés: "+message);}
    private record Candidate(Source source,int score){}
}
