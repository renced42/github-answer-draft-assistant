package hu.gov.nav.answerdraft;

import java.util.*;

public final class Main {
    public static void main(String[] args){
        Config c=Config.fromEnvironment();String limitedBody=limitQuestion(c.body(),c.questionMaxChars());Question q=new Question(c.eventKind(),c.repository(),c.number(),c.title(),limitedBody,c.questionUrl(),c.author());WebClient http=new WebClient();
        System.out.println("GitHub API hitelesítés: "+(c.githubToken().isBlank()?"nincs – nyilvános, alacsonyabb limitű hozzáférés":"aktív"));
        System.out.println("Nyilvános GitHub-források keresése: "+c.organization());GitHubSourceCollector githubCollector=new GitHubSourceCollector(http,c);List<Source> sources=new ArrayList<>(githubCollector.collect(q));
        if(c.navSearch()){System.out.println("Nyilvános NAV-webforrások keresése...");sources.addAll(new NavSourceCollector(http).collect(q));}
        KnowledgeRepositoryClient knowledge=new KnowledgeRepositoryClient(http,c);
        if(knowledge.enabled()){System.out.println("Jóváhagyott tudás keresése: "+c.knowledgeRepository());List<Source> approved=knowledge.collectApproved(q);sources.addAll(approved);System.out.println("Felhasznált jóváhagyott tudás-issue-k: "+approved.size());}
        sources=rankAndDedupe(sources);
        AiClient client="openai".equals(c.aiProvider())?new OpenAiClient(http,c):new GroqClient(http,c);
        System.out.println("Összegyűjtött források: "+sources.size());System.out.println("AI-szolgáltató: "+c.aiProvider());AiClient.Generation generation=client.generate(PromptBuilder.build(q,sources));List<String> warnings=new ArrayList<>(githubCollector.warnings());warnings.addAll(knowledge.warnings());warnings.addAll(generation.warnings());
        String reviewUrl=knowledge.createReviewIssue(q,generation.content(),sources,warnings);warnings=new ArrayList<>(warnings);for(String warning:knowledge.warnings())if(!warnings.contains(warning))warnings.add(warning);
        if(!reviewUrl.isBlank())System.out.println("Privát review issue létrehozva: "+reviewUrl);
        EmailComposer.Email email=EmailComposer.compose(q,generation.content(),sources,warnings,reviewUrl);
        if(c.dryRun()){System.out.println("DRY RUN – email nem kerül elküldésre.\n\n"+email.subject()+"\n\n"+email.body());}else{new SmtpMailer().send(c,email);System.out.println("A választervezet elküldve "+c.recipients().size()+" címzettnek. GitHub-publikálás nem történt.");}
    }
    static List<Source> rankAndDedupe(List<Source> sources){LinkedHashMap<String,Source> unique=new LinkedHashMap<>();for(Source source:sources)if(!source.content().isBlank()&&(source.privateKnowledge()||!source.url().isBlank())){String key=source.privateKnowledge()?"private:"+source.content():source.url();unique.merge(key,source,(left,right)->left.score()>=right.score()?left:right);}return unique.values().stream().sorted(Comparator.comparingInt(Source::score).reversed()).toList();}
    static String limitQuestion(String text,int maxChars){
        String value=text==null?"":text;int count=value.codePointCount(0,value.length());if(count<=maxChars)return value;
        int end=value.offsetByCodePoints(0,maxChars);System.out.println("Figyelmeztetés: a kérdés szövege "+count+" karakter; az AI-szolgáltatónak csak az első "+maxChars+" karakter kerül átadásra.");
        return value.substring(0,end)+"\n\n[FIGYELMEZTETÉS: Az eredeti kérdés hosszabb volt; az AI-bemenet "+maxChars+" karakternél le lett rövidítve. A teljes szöveg az eredeti GitHub-linken olvasható.]";
    }
}
