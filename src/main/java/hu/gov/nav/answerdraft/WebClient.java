package hu.gov.nav.answerdraft;

import java.net.*; import java.net.http.*; import java.nio.charset.StandardCharsets; import java.time.Duration; import java.util.*;

final class WebClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
    record Response(int status,String body) {}
    Response get(String url, Map<String,String> headers) { return send("GET",url,null,headers); }
    Response postJson(String url,Object body,Map<String,String> headers) { Map<String,String> h=new LinkedHashMap<>(headers); h.put("Content-Type","application/json"); return send("POST",url,Json.stringify(body),h); }
    private Response send(String method,String url,String body,Map<String,String> headers) {
        for(int attempt=1;attempt<=4;attempt++) try {
            HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).header("User-Agent","github-answer-draft-assistant/2");
            headers.forEach(b::header); b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
            HttpResponse<String> r=client.send(b.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if((r.statusCode()==429||r.statusCode()==408||r.statusCode()>=500)&&attempt<4){ Thread.sleep(attempt*1500L); continue; }
            if(r.statusCode()<200||r.statusCode()>=300) throw new HttpStatusException(r.statusCode(),"HTTP "+r.statusCode()+" hiba a(z) "+url+" hívásakor: "+shorten(r.body(),1200));
            return new Response(r.statusCode(),r.body());
        } catch(HttpStatusException x){throw x;} catch(InterruptedException x){Thread.currentThread().interrupt();throw new IllegalStateException("Megszakított HTTP kérés",x);} catch(Exception x){if(attempt==4)throw new IllegalStateException("HTTP hívás sikertelen: "+url,x); try{Thread.sleep(attempt*1000L);}catch(InterruptedException i){Thread.currentThread().interrupt();}}
        throw new IllegalStateException("HTTP hívás sikertelen: "+url);
    }
    private static final class HttpStatusException extends IllegalStateException {
        @SuppressWarnings("unused") private final int status;
        private HttpStatusException(int status,String message){super(message);this.status=status;}
    }
    static String query(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
    static String shorten(String s,int max){return s==null?"":s.substring(0,Math.min(max,s.length()));}
}
