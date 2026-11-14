package com.stillgrove.stillgroveapi.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class AiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public AiClient(ObjectMapper json,
                    @Value("${stillgrove.ai.api-key:}") String apiKey,
                    @Value("${stillgrove.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
                    @Value("${stillgrove.ai.model:qwen-plus}") String model) {
        this.json=json; this.apiKey=apiKey; this.baseUrl=baseUrl; this.model=model;
    }
    public boolean available() { return !apiKey.isBlank(); }
    public String model() { return model; }

    public String complete(List<Map<String,String>> messages) throws Exception {
        String body=json.writeValueAsString(Map.of("model",model,"messages",messages,"stream",false));
        var request=HttpRequest.newBuilder(URI.create(baseUrl+"/chat/completions"))
                .timeout(Duration.ofMinutes(2)).header("Authorization","Bearer "+apiKey)
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IllegalStateException("模型服务返回 HTTP "+response.statusCode());
        return json.readTree(response.body()).path("choices").get(0).path("message").path("content").asString();
    }

    public List<Double> embed(String content) throws Exception {
        String body=json.writeValueAsString(Map.of("model","text-embedding-v4","input",content,"dimensions",1024));
        var request=HttpRequest.newBuilder(URI.create(baseUrl+"/embeddings"))
                .timeout(Duration.ofSeconds(40)).header("Authorization","Bearer "+apiKey)
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200) throw new IllegalStateException("Embedding 服务返回 HTTP "+response.statusCode());
        var vector=json.readTree(response.body()).path("data").get(0).path("embedding");
        var values=new java.util.ArrayList<Double>();
        for(var item:vector) values.add(item.asDouble());
        return values;
    }

    public void stream(List<Map<String,String>> messages, Consumer<String> onText) throws Exception {
        String body=json.writeValueAsString(Map.of("model",model,"messages",messages,"stream",true));
        HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl+"/chat/completions"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization","Bearer "+apiKey)
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofLines());
        if(response.statusCode()!=200) throw new IllegalStateException("模型服务返回 HTTP "+response.statusCode());
        try(var lines=response.body()) {
            for(String line : (Iterable<String>) lines::iterator) {
                if(!line.startsWith("data: ")) continue;
                String data=line.substring(6);
                if("[DONE]".equals(data)) break;
                var root=json.readTree(data);
                var choices=root.path("choices");
                if(!choices.isArray() || choices.isEmpty()) continue;
                var content=choices.get(0).path("delta").path("content");
                if(content.isString() && !content.asString().isEmpty()) onText.accept(content.asString());
            }
        }
    }
}
