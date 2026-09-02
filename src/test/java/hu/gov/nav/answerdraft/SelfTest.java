package hu.gov.nav.answerdraft;

import java.util.*;

public final class SelfTest {
    public static void main(String[] args){
        Object parsed=Json.parse(Json.stringify(Map.of("text","árvíz\nXML","items",Arrays.asList(1,true,null))));
        check("árvíz\nXML".equals(Json.string(Json.object(parsed).get("text"))),"JSON körút");
        Question q=new Question("issue","renced42/test",1,"Hogyan tölthetek fel eÁFA analitikát?","Kérek XML példát.","https://github.com/renced42/test/issues/1","renced42");
        List<String> keys=KeywordExtractor.extract(q);check(keys.contains("VatAnalytics")&&keys.contains("xsd"),"szakterületi kulcsszavak");
        Prompt prompt=PromptBuilder.build(q,List.of(new Source("schema","https://github.com/nav-gov-hu/test/schema.xsd","<xs:schema/>",100)));
        check(prompt.system().contains("szemléltető, generált példa")&&prompt.system().contains("külön rendszer"),"rendszerprompt korlátozások");
        check(prompt.user().contains("nav-gov-hu/test")&&prompt.user().contains("<xs:schema/>"),"felhasználói prompt tartalma");
        String privateKnowledgeUrl="https://github.com/renced42/knowledge/issues/1";
        Prompt balanced=PromptBuilder.build(q,List.of(new Source("Hivatalos 1","https://nav.gov.hu/1","egy",100),new Source("Hivatalos 2","https://nav.gov.hu/2","kettő",90),new Source("Jóváhagyott tudás: korábbi",privateKnowledgeUrl,"Tudás eredete: expert-confirmed\nNyilvános dokumentáció állapota: documentation-gap\nellenőrzött",160,SourceType.APPROVED_KNOWLEDGE)));
        check(balanced.user().indexOf("Hivatalos 1")<balanced.user().indexOf("Jóváhagyott tudás")&&balanced.user().contains("ellenőrzött"),"hivatalos és jóváhagyott források kiegyensúlyozása");
        check(!balanced.user().contains(privateKnowledgeUrl)&&balanced.user().contains("ÜGYFÉLVÁLASZBAN NEM HIVATKOZHATÓ"),"privát tudástár URL kizárása a modellpromptból");
        check(balanced.system().contains("expert-confirmed")&&balanced.system().contains("documentation-gap"),"szakértői tudás promptszabályai");
        Source privateSource=new Source("Jóváhagyott belső tudás","","ellenőrzött válasz",160,SourceType.APPROVED_KNOWLEDGE);
        check(Main.rankAndDedupe(List.of(privateSource)).size()==1,"URL nélküli privát tudás megőrzése");
        Map<String,Object> openAiRequest=OpenAiClient.requestBody("gpt-5.4-mini",prompt,true);
        check("gpt-5.4-mini".equals(openAiRequest.get("model"))&&Json.stringify(openAiRequest).contains("web_search"),"OpenAI Responses API kérés");
        String openAiResponse="{\"output\":[{\"type\":\"web_search_call\"},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"OpenAI válasz\"}]}]}";
        check("OpenAI válasz".equals(OpenAiClient.parseOutput(openAiResponse)),"OpenAI Responses API válaszfeldolgozás");
        EmailComposer.Email email=EmailComposer.compose(q,"## JAVASOLT VÁLASZ\nTeszt",List.of());check(email.body().contains(q.url())&&email.subject().contains("#1"),"email és kérdéslink");
        EmailComposer.Email warned=EmailComposer.compose(q,"Tervezet",List.of(),List.of("GitHub keresés részleges."));check(warned.body().contains("KERESÉSI FIGYELMEZTETÉS")&&warned.body().contains("részleges"),"keresési figyelmeztetés az emailben");
        String limited=Main.limitQuestion("á".repeat(3010),3000);check(limited.startsWith("á".repeat(3000))&&limited.contains("le lett rövidítve"),"kérdéshossz korlátozása");
        check("eNyugta".equals(SystemClassifier.classify("Jól látjuk, hogy az eNyugta végpont TLS 1.3?")),"eNyugta rendszerazonosítás");
        check("Online pénztárgép".equals(SystemClassifier.classify("Online pénztárgép TLS beállítása")),"pénztárgép rendszerazonosítás");
        String review=KnowledgeRepositoryClient.reviewBody(q,"eÁFA","AI tervezet",List.of(new Source("XSD","https://example.test/a.xsd","x",10)),List.of(),"approved-knowledge");
        check(review.contains("/approve")&&review.contains("Labels")&&review.contains("official-source")&&review.contains("documentation-gap")&&review.contains("approved-knowledge"),"review issue sablon");
        String completed="## Ellenőrzött végleges válasz\n\nEz az ellenőrzött válasz.\n\n## Tudás eredete\n\nexpert-confirmed";
        check("Ez az ellenőrzött válasz.".equals(KnowledgeRepositoryClient.section(completed,"## Ellenőrzött végleges válasz","## Tudás eredete")),"végleges válasz kinyerése");
        check("Ez az ellenőrzött válasz.".equals(KnowledgeRepositoryClient.namedSection(completed,"Ellenőrzött végleges válasz")),"név szerinti szakaszkinyerés");
        check("Szakértő által ellenőrzött válasz.".equals(KnowledgeRepositoryClient.approvalCommentAnswer(List.of(Map.of("body","Megjegyzés"),Map.of("body","/approve\nSzakértő által ellenőrzött válasz.")))),"/approve hozzászólás feldolgozása");
        String issueForm="### Azonosított rendszer\n\neÁFA\n\n### Ellenőrzött végleges válasz\n\nŰrlapból rögzített válasz.\n\n### Tudás eredete\n\nexpert-confirmed\n\n### Nyilvános dokumentáció állapota\n\ndocumentation-gap\n\n### Felhasznált források\n\nhttps://nav.gov.hu";
        check("Űrlapból rögzített válasz.".equals(KnowledgeRepositoryClient.namedSection(issueForm,"Ellenőrzött végleges válasz")),"Issue Form szakaszkinyerés");
        check("expert-confirmed".equals(KnowledgeRepositoryClient.namedSection(issueForm,"Tudás eredete")),"tudáseredet kinyerése");
        check(!KnowledgeRepositoryClient.sanitizePrivateKnowledgeLinks("Lásd https://github.com/renced42/github-answer-knowledge/issues/12#issuecomment-1","renced42/github-answer-knowledge").contains("issues/12"),"privát tudástár-link tisztítása");
        EmailComposer.Email reviewEmail=EmailComposer.compose(q,"Tervezet",List.of(),List.of(),"https://github.com/renced42/knowledge/issues/3");
        check(reviewEmail.body().contains("PRIVÁT REVIEW")&&reviewEmail.body().contains("knowledge/issues/3"),"review link az emailben");
        System.out.println("Minden önellenőrzés sikeres.");
    }
    private static void check(boolean value,String name){if(!value)throw new AssertionError("Sikertelen teszt: "+name);}
}
