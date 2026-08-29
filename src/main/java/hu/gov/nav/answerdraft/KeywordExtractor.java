package hu.gov.nav.answerdraft;

import java.text.Normalizer; import java.util.*; import java.util.regex.*;

final class KeywordExtractor {
    private static final Set<String> STOP=Set.of("hogyan","tudok","tudnal","milyen","kell","hasznalni","rendszerben","kapcsan","pelda","adni","erre","es","az","egy","nem","van","vagy","hogy","lehet","kerem");
    static List<String> extract(Question q){
        String text=normalize(q.title()+" "+q.body()); LinkedHashSet<String> out=new LinkedHashSet<>();
        Matcher m=Pattern.compile("[a-z0-9_-]{3,}").matcher(text); while(m.find()&&out.size()<8){String w=m.group();if(!STOP.contains(w))out.add(w);}
        if(text.contains("analitik")||text.contains("eafa")) Collections.addAll(out,"VatAnalytics","earData","xml","xsd","minta");
        if(text.contains("token")||text.contains("auth")) Collections.addAll(out,"tokenExchange","requestVersion","authentication");
        if(text.contains("enyugta")||text.contains("nyugta")) Collections.addAll(out,"receiptData","receipt","schema","xsd");
        if(text.contains("hash")) Collections.addAll(out,"hash","sha3-512","upload");
        return out.stream().limit(12).toList();
    }
    private static String normalize(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
}
