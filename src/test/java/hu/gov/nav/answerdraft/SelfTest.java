package hu.gov.nav.answerdraft;

import java.util.*;

public final class SelfTest {
    public static void main(String[] args){
        Object parsed=Json.parse(Json.stringify(Map.of("text","árvíz\nXML","items",Arrays.asList(1,true,null))));
        check("árvíz\nXML".equals(Json.string(Json.object(parsed).get("text"))),"JSON körút");
        Question q=new Question("issue","renced42/test",1,"Hogyan tölthetek fel eÁFA analitikát?","Kérek XML példát.","https://github.com/renced42/test/issues/1","renced42");
        List<String> keys=KeywordExtractor.extract(q);check(keys.contains("VatAnalytics")&&keys.contains("xsd"),"szakterületi kulcsszavak");
        String prompt=PromptBuilder.build(q,List.of(new Source("schema","https://github.com/nav-gov-hu/test/schema.xsd","<xs:schema/>",100)));
        check(prompt.contains("szemléltető, generált példa")&&prompt.contains("nav-gov-hu"),"prompt korlátozások");
        EmailComposer.Email email=EmailComposer.compose(q,"## JAVASOLT VÁLASZ\nTeszt",List.of());check(email.body().contains(q.url())&&email.subject().contains("#1"),"email és kérdéslink");
        EmailComposer.Email warned=EmailComposer.compose(q,"Tervezet",List.of(),List.of("GitHub keresés részleges."));check(warned.body().contains("KERESÉSI FIGYELMEZTETÉS")&&warned.body().contains("részleges"),"keresési figyelmeztetés az emailben");
        String limited=Main.limitQuestion("á".repeat(3010),3000);check(limited.startsWith("á".repeat(3000))&&limited.contains("le lett rövidítve"),"kérdéshossz korlátozása");
        System.out.println("Minden önellenőrzés sikeres.");
    }
    private static void check(boolean value,String name){if(!value)throw new AssertionError("Sikertelen teszt: "+name);}
}
