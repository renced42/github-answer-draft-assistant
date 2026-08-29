package hu.gov.nav.answerdraft;

import java.util.*;

public final class Main {
    public static void main(String[] args){
        Config c=Config.fromEnvironment();String limitedBody=limitQuestion(c.body(),c.questionMaxChars());Question q=new Question(c.eventKind(),c.repository(),c.number(),c.title(),limitedBody,c.questionUrl(),c.author());WebClient http=new WebClient();
        System.out.println("Nyilvános GitHub-források keresése: "+c.organization());List<Source> sources=new ArrayList<>(new GitHubSourceCollector(http,c).collect(q));
        if(c.navSearch()){System.out.println("Nyilvános NAV-webforrások keresése...");sources.addAll(new NavSourceCollector(http).collect(q));}
        System.out.println("Összegyűjtött források: "+sources.size());String draft=new GroqClient(http,c).generate(PromptBuilder.build(q,sources));EmailComposer.Email email=EmailComposer.compose(q,draft,sources);
        if(c.dryRun()){System.out.println("DRY RUN – email nem kerül elküldésre.\n\n"+email.subject()+"\n\n"+email.body());}else{new SmtpMailer().send(c,email);System.out.println("A választervezet elküldve "+c.recipients().size()+" címzettnek. GitHub-publikálás nem történt.");}
    }
    static String limitQuestion(String text,int maxChars){
        String value=text==null?"":text;int count=value.codePointCount(0,value.length());if(count<=maxChars)return value;
        int end=value.offsetByCodePoints(0,maxChars);System.out.println("Figyelmeztetés: a kérdés szövege "+count+" karakter; a Groqnak csak az első "+maxChars+" karakter kerül átadásra.");
        return value.substring(0,end)+"\n\n[FIGYELMEZTETÉS: Az eredeti kérdés hosszabb volt; az AI-bemenet "+maxChars+" karakternél le lett rövidítve. A teljes szöveg az eredeti GitHub-linken olvasható.]";
    }
}
