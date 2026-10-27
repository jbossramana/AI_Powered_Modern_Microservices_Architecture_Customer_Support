package com.example.support.service;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class MlClassifierClient {
    private final RestClient client;
    public MlClassifierClient(RestClient.Builder builder){client=builder.baseUrl("http://localhost:8001").build();}
    public MlPrediction predict(String text) {
        MlResponse r=client.post().uri("/predict").body(new MlRequest(text)).retrieve().body(MlResponse.class);
        if(r==null) throw new IllegalStateException("ML service returned empty response");
        return new MlPrediction(r.category(),r.priority(),r.confidence());
    }
    public record MlRequest(String text) {}
    public record MlResponse(String category,String priority,double confidence) {}
    public record MlPrediction(String category,String priority,double confidence) {}
}
