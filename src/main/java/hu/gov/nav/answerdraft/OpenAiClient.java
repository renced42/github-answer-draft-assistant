package hu.gov.nav.answerdraft;

import java.util.*;

final class OpenAiClient implements AiClient {
    private final WebClient http;
    private final Config config;

    OpenAiClient(WebClient http,Config config){this.http=http;this.config=config;}

    @Override public Generation generate(Prompt prompt){
        if(!config.browserSearch())return new Generation(request(prompt,false),List.of());
        try{return new Generation(request(prompt,true),List.of());}
        catch(WebClient.HttpStatusException error){
            if(error.status()!=400)throw error;
            String warning="Az OpenAI web_search eszköz hívása HTTP 400 hibát adott. A tervezet a Java által összegyűjtött GitHub-, NAV-web- és jóváhagyott tudásforrásokból készült.";
            System.out.println("Figyelmeztetés: "+warning);
            return new Generation(request(prompt,false),List.of(warning));
        }
    }

    private String request(Prompt prompt,boolean webSearch){
        Map<String,Object> request=requestBody(config.openAiModel(),prompt,webSearch);
        String body=http.postJson("https://api.openai.com/v1/responses",request,Map.of("Authorization","Bearer "+config.openAiKey())).body();
        return parseOutput(body);
    }

    static Map<String,Object> requestBody(String model,Prompt prompt,boolean webSearch){
        Map<String,Object> request=new LinkedHashMap<>();
        request.put("model",model);
        request.put("instructions",prompt.system());
        request.put("input",prompt.user());
        request.put("reasoning",Map.of("effort","low"));
        request.put("max_output_tokens",2500);
        if(webSearch){
            request.put("tools",List.of(Map.of("type","web_search","filters",Map.of("allowed_domains",List.of("github.com","raw.githubusercontent.com","nav.gov.hu")))));
            request.put("tool_choice","auto");
        }
        return request;
    }

    static String parseOutput(String body){
        Map<String,Object> root=Json.object(Json.parse(body));
        String direct=Json.string(root.get("output_text"));
        if(!direct.isBlank())return direct.trim();
        StringBuilder text=new StringBuilder();
        for(Object rawItem:Json.array(root.get("output"))){
            Map<String,Object> item=Json.object(rawItem);
            if(!"message".equals(Json.string(item.get("type"))))continue;
            for(Object rawContent:Json.array(item.get("content"))){
                Map<String,Object> content=Json.object(rawContent);
                if(!"output_text".equals(Json.string(content.get("type"))))continue;
                String part=Json.string(content.get("text"));
                if(!part.isBlank()){if(!text.isEmpty())text.append('\n');text.append(part);}
            }
        }
        if(text.isEmpty())throw new IllegalStateException("Az OpenAI Responses API nem adott szöveges választ.");
        return text.toString().trim();
    }
}
