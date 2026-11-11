
package com.acmesupport.mlclient.adapter;

import com.acmesupport.mlclient.port.PriorityPredictionPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MlModelPriorityAdapter implements PriorityPredictionPort {

    private final RestClient mlServiceClient;

    public MlModelPriorityAdapter(
            @Qualifier("mlServiceClient") RestClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    private record PredictResponse(
            String priority,
            double confidence,
            @JsonProperty("class_probabilities")
            Map<String, Double> classProbabilities) {
    }

    @Override
    public PriorityPrediction predict(
            String subject,
            String body,
            String planTier,
            int accountAgeDays,
            int priorTicketCount30d) {

        String json = """
                {
                  "subject": %s,
                  "body": %s,
                  "plan_tier": %s,
                  "account_age_days": %d,
                  "prior_ticket_count_30d": %d
                }
                """.formatted(
                jsonString(subject),
                jsonString(body),
                jsonString(planTier),
                accountAgeDays,
                priorTicketCount30d
        );

        byte[] payload = json.getBytes(StandardCharsets.UTF_8);

        System.out.println("Python ML endpoint: /predict");
        System.out.println("Request payload size: " + payload.length);

        PredictResponse response = mlServiceClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .contentLength(payload.length)
                .body(payload)
                .retrieve()
                .body(PredictResponse.class);

        if (response == null || response.priority() == null) {
            throw new IllegalStateException(
                    "Python ML service returned an invalid prediction response");
        }

        return new PriorityPrediction(
                response.priority(),
                response.confidence(),
                response.classProbabilities() == null
                        ? Map.of()
                        : response.classProbabilities()
        );
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder result = new StringBuilder("\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }

        return result.append('"').toString();
    }
}
