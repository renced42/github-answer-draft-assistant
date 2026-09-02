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
        Prompt balanced=PromptBuilder.build(q,List.of(new Source("Hivatalos 1","https://nav.gov.hu/1","egy",100),new Source("Hivatalos 2","https://nav.gov.hu/2","kettő",90),new Source("Jóváhagyott tudás: korábbi","https://github.com/renced42/knowledge/issues/1","ellenőrzött",160)));
        check(balanced.user().indexOf("Hivatalos 1")<balanced.user().indexOf("Jóváhagyott tudás")&&balanced.user().contains("ellenőrzött"),"hivatalos és jóváhagyott források kiegyensúlyozása");
        EmailComposer.Email email=EmailComposer.compose(q,"## JAVASOLT VÁLASZ\nTeszt",List.of());check(email.body().contains(q.url())&&email.subject().contains("#1"),"email és kérdéslink");
        EmailComposer.Email warned=EmailComposer.compose(q,"Tervezet",List.of(),List.of("GitHub keresés részleges."));check(warned.body().contains("KERESÉSI FIGYELMEZTETÉS")&&warned.body().contains("részleges"),"keresési figyelmeztetés az emailben");
        String limited=Main.limitQuestion("á".repeat(3010),3000);check(limited.startsWith("á".repeat(3000))&&limited.contains("le lett rövidítve"),"kérdéshossz korlátozása");
        check("eNyugta".equals(SystemClassifier.classify("Jól látjuk, hogy az eNyugta végpont TLS 1.3?")),"eNyugta rendszerazonosítás");
        check("Online pénztárgép".equals(SystemClassifier.classify("Online pénztárgép TLS beállítása")),"pénztárgép rendszerazonosítás");
        String review=KnowledgeRepositoryClient.reviewBody(q,"eÁFA","AI tervezet",List.of(new Source("XSD","https://example.test/a.xsd","x",10)),List.of(),"approved-knowledge");
        check(review.contains("[IDE ÍRD AZ ELLENŐRZÖTT VÉGLEGES VÁLASZT]")&&review.contains("approved-knowledge"),"review issue sablon");
        String completed=review.replace("[IDE ÍRD AZ ELLENŐRZÖTT VÉGLEGES VÁLASZT]","Ez az ellenőrzött válasz.");
        check("Ez az ellenőrzött válasz.".equals(KnowledgeRepositoryClient.section(completed,"## Ellenőrzött végleges válasz","## Felhasznált források")),"végleges válasz kinyerése");
        check("Ez az ellenőrzött válasz.".equals(KnowledgeRepositoryClient.namedSection(completed,"Ellenőrzött végleges válasz")),"név szerinti szakaszkinyerés");
        String issueForm="### Azonosított rendszer\n\neÁFA\n\n### Ellenőrzött végleges válasz\n\nŰrlapból rögzített válasz.\n\n### Felhasznált források\n\nhttps://nav.gov.hu";
        check("Űrlapból rögzített válasz.".equals(KnowledgeRepositoryClient.namedSection(issueForm,"Ellenőrzött végleges válasz")),"Issue Form szakaszkinyerés");
        EmailComposer.Email reviewEmail=EmailComposer.compose(q,"Tervezet",List.of(),List.of(),"https://github.com/renced42/knowledge/issues/3");
        check(reviewEmail.body().contains("PRIVÁT REVIEW")&&reviewEmail.body().contains("knowledge/issues/3"),"review link az emailben");
        System.out.println("Minden önellenőrzés sikeres.");
    }
    private static void check(boolean value,String name){if(!value)throw new AssertionError("Sikertelen teszt: "+name);}
}
