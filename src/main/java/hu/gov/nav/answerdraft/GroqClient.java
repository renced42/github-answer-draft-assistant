package hu.gov.nav.answerdraft;

import java.util.*;

final class GroqClient {
    record Generation(String content,List<String> warnings){}
    private final WebClient http; private final Config config;
    GroqClient(WebClient http,Config config){this.http=http;this.config=config;}
    Generation generate(String prompt){
        if(!config.browserSearch())return new Generation(request(prompt,false,0.15),List.of());
        try{return new Generation(request(prompt,true,0.15),List.of());}
        catch(WebClient.HttpStatusException first){
            if(!isOutputParseFailure(first))throw first;
            System.out.println("Figyelmeztetés: a Groq böngészős keresés kimenetét nem tudta feldolgozni; egyszer újrapróbáljuk.");
            try{return new Generation(request(prompt+"\n\nA böngészőeszközt kizárólag a szolgáltató által elvárt strukturált eszközhívással használd.",true,0.35),List.of());}
            catch(WebClient.HttpStatusException second){
                if(!isOutputParseFailure(second))throw second;
                String warning="A Groq beépített böngészős keresése kétszer output_parse_failed hibát adott. A tervezet a Java által előzetesen összegyűjtött GitHub- és NAV-webforrásokból készült; az internetes forrásfeltárás részleges lehet.";
                System.out.println("Figyelmeztetés: "+warning);
                return new Generation(request(prompt,false,0.15),List.of(warning));
            }
        }
    }
    private String request(String prompt,boolean browserSearch,double temperature){
        Map<String,Object> request=new LinkedHashMap<>();request.put("model",config.groqModel());request.put("messages",List.of(Map.of("role","user","content",prompt)));request.put("temperature",temperature);request.put("max_completion_tokens",1200);request.put("stream",false);request.put("reasoning_effort","low");
        if(browserSearch){request.put("tools",List.of(Map.of("type","browser_search")));request.put("tool_choice","required");}
        String body=http.postJson("https://api.groq.com/openai/v1/chat/completions",request,Map.of("Authorization","Bearer "+config.groqKey())).body();
        Map<String,Object> root=Json.object(Json.parse(body));List<Object> choices=Json.array(root.get("choices"));if(choices.isEmpty())throw new IllegalStateException("A Groq nem adott válaszjelöltet.");
        String content=Json.string(Json.object(Json.object(choices.get(0)).get("message")).get("content"));if(content.isBlank())throw new IllegalStateException("A Groq üres választ adott.");return content.trim();
    }
    private boolean isOutputParseFailure(WebClient.HttpStatusException error){return error.status()==400&&error.getMessage()!=null&&error.getMessage().contains("output_parse_failed");}
}
