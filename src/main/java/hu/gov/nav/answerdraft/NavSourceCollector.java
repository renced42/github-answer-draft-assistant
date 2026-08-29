package hu.gov.nav.answerdraft;

import java.net.URI; import java.util.*; import java.util.regex.*;

final class NavSourceCollector {
    private static final List<String> SEEDS=List.of("https://nav.gov.hu/ado/afa/eafa","https://nav.gov.hu/ado/enyugta","https://eafa.nav.gov.hu/");
    private final WebClient http;
    NavSourceCollector(WebClient http){this.http=http;}
    List<Source> collect(Question q){
        List<String> keys=KeywordExtractor.extract(q);List<Source> out=new ArrayList<>();Set<String> visited=new HashSet<>();ArrayDeque<String> queue=new ArrayDeque<>(SEEDS);
        while(!queue.isEmpty()&&visited.size()<12&&out.size()<5){String url=queue.remove();if(!allowed(url)||!visited.add(url))continue;try{String html=http.get(url,Map.of()).body();String text=clean(html);int score=score(text,keys);if(score>0)out.add(new Source("NAV weboldal: "+title(html),url,WebClient.shorten(text,9000),55+score));for(Link l:links(url,html))if(allowed(l.url)&&(l.url.endsWith(".pdf")||score(l.label,keys)>0)){if(l.url.endsWith(".pdf"))out.add(new Source("NAV PDF: "+l.label,l.url,"Hivatalos NAV PDF. A tartalmát a modell böngészős keresése ellenőrizze közvetlenül.",65+score(l.label,keys)));else queue.add(l.url);}}catch(Exception x){System.out.println("Figyelmeztetés: NAV-oldal kihagyva: "+url);}}
        return out.stream().distinct().limit(6).toList();
    }
    private boolean allowed(String url){try{URI u=URI.create(url);String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("nav.gov.hu")||h.endsWith(".nav.gov.hu"));}catch(Exception x){return false;}}
    private int score(String text,List<String> keys){String l=text.toLowerCase(Locale.ROOT);int s=0;for(String k:keys)if(l.contains(k.toLowerCase(Locale.ROOT)))s+=5;return s;}
    private String clean(String html){return html.replaceAll("(?is)<script.*?</script>|<style.*?</style>"," ").replaceAll("(?s)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replaceAll("\\s+"," ").trim();}
    private String title(String html){Matcher m=Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);return m.find()?clean(m.group(1)):"NAV";}
    private List<Link> links(String base,String html){List<Link> out=new ArrayList<>();Matcher m=Pattern.compile("(?is)<a[^>]+href=[\"']([^\"'#]+)[\"'][^>]*>(.*?)</a>").matcher(html);while(m.find()&&out.size()<80)try{out.add(new Link(URI.create(base).resolve(m.group(1)).toString(),clean(m.group(2))));}catch(Exception ignored){}return out;}
    private record Link(String url,String label){}
}
