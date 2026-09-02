package hu.gov.nav.answerdraft;

import java.text.Normalizer;
import java.util.Locale;

final class SystemClassifier {
    private SystemClassifier() {}

    static String classify(Question question) {
        return classify(question.title()+" "+question.body());
    }

    static String classify(String text) {
        String value=normalize(text);
        if(containsAny(value,"enyugta","e-nyugta","receiptdata","nyugtaadat"))return "eNyugta";
        if(containsAny(value,"eafa","e-afa","vatanalytics","eafa m2m","analitika feltoltes"))return "eÁFA";
        if(containsAny(value,"online szamla","onlineszamla","invoice data"))return "Online Számla";
        if(containsAny(value,"online penztargep","opg","penztargep"))return "Online pénztárgép";
        return "NEM AZONOSÍTOTT";
    }

    static boolean sameSystem(String expected,String issueText) {
        if("NEM AZONOSÍTOTT".equals(expected))return true;
        return expected.equals(classify(issueText));
    }

    private static boolean containsAny(String value,String... terms){for(String term:terms)if(value.contains(term))return true;return false;}
    private static String normalize(String value){return Normalizer.normalize(value==null?"":value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
}
