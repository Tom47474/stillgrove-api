package com.stillgrove.stillgroveapi.memory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QdrantClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json;
    private final String url;
    private final String key;
    private static final String COLLECTION="stillgrove_memories";
    public QdrantClient(ObjectMapper json,@Value("${stillgrove.qdrant.url:http://localhost:6333}") String url,
                        @Value("${stillgrove.qdrant.api-key:}") String key) {
        this.json=json; this.url=url; this.key=key;
    }
    private HttpRequest.Builder request(String path) {
        var builder=HttpRequest.newBuilder(URI.create(url+path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/json");
        if(!key.isBlank()) builder.header("api-key",key);
        return builder;
    }
    private String send(HttpRequest request) throws Exception {
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()/100!=2) throw new IllegalStateException("Qdrant HTTP "+response.statusCode());
        return response.body();
    }
    public boolean healthy() {
        try {
            var response=http.send(request("/").GET().build(),HttpResponse.BodyHandlers.discarding());
            return response.statusCode()/100==2;
        } catch(Exception ex) { return false; }
    }
    public void ensureCollection() throws Exception {
        var response=http.send(request("/collections/"+COLLECTION).GET().build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()==200) return;
        if(response.statusCode()!=404) throw new IllegalStateException("Qdrant HTTP "+response.statusCode());
        send(request("/collections/"+COLLECTION).PUT(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(Map.of("vectors",Map.of("size",1024,"distance","Cosine"))))).build());
        send(request("/collections/"+COLLECTION+"/index?wait=true").PUT(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(Map.of("field_name","user_id","field_schema","integer")))).build());
    }
    public void upsert(String pointId,long userId,long memoryId,int version,List<Double> vector) throws Exception {
        ensureCollection();
        var point=Map.of("id",pointId,"vector",vector,"payload",Map.of("user_id",userId,"memory_id",memoryId,"version",version));
        send(request("/collections/"+COLLECTION+"/points?wait=true").PUT(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(Map.of("points",List.of(point))))).build());
    }
    public void delete(String pointId) throws Exception {
        var collection=http.send(request("/collections/"+COLLECTION).GET().build(),HttpResponse.BodyHandlers.ofString());
        if(collection.statusCode()==404) return;
        if(collection.statusCode()!=200) throw new IllegalStateException("Qdrant HTTP "+collection.statusCode());
        send(request("/collections/"+COLLECTION+"/points/delete?wait=true").POST(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(Map.of("points",List.of(pointId))))).build());
    }
    public List<Map<String,Object>> search(long userId,List<Double> vector) throws Exception {
        var body=Map.of("query",vector,"limit",5,"with_payload",true,
                "filter",Map.of("must",List.of(Map.of("key","user_id","match",Map.of("value",userId)))));
        String result=send(request("/collections/"+COLLECTION+"/points/query").POST(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(body))).build());
        var points=json.readTree(result).path("result").path("points");
        var found=new ArrayList<Map<String,Object>>();
        for(var point:points) {
            var payload=point.path("payload");
            found.add(Map.of("id",payload.path("memory_id").asLong(),"version",payload.path("version").asInt()));
        }
        return found;
    }
}
