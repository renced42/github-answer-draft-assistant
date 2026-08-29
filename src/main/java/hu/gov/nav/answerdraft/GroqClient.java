package hu.gov.nav.answerdraft;

import java.util.*;

final class GroqClient {
    private final WebClient http; private final Config config;
    GroqClient(WebClient http,Config config){this.http=http;this.config=config;}
    String generate(String prompt){
        Map<String,Object> request=new LinkedHashMap<>();request.put("model",config.groqModel());request.put("messages",List.of(Map.of("role","user","content",prompt)));request.put("temperature",0.15);request.put("max_completion_tokens",2200);request.put("stream",false);request.put("reasoning_effort","low");
        if(config.browserSearch()){request.put("tools",List.of(Map.of("type","browser_search")));request.put("tool_choice","required");}
        String body=http.postJson("https://api.groq.com/openai/v1/chat/completions",request,Map.of("Authorization","Bearer "+config.groqKey())).body();
        Map<String,Object> root=Json.object(Json.parse(body));List<Object> choices=Json.array(root.get("choices"));if(choices.isEmpty())throw new IllegalStateException("A Groq nem adott válaszjelöltet.");
        String content=Json.string(Json.object(Json.object(choices.get(0)).get("message")).get("content"));if(content.isBlank())throw new IllegalStateException("A Groq üres választ adott.");return content.trim();
    }
}
