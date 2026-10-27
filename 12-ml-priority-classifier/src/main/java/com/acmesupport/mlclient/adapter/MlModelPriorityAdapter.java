package com.acmesupport.mlclient.adapter;

import com.acmesupport.mlclient.port.PriorityPredictionPort;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * The only class in this module that knows the priority prediction comes from an HTTP
 * call to a Python service, let alone that a RandomForestClassifier is behind it. Note
 * the @JsonProperty annotations below: FastAPI's Pydantic model uses Python's
 * snake_case convention (plan_tier, account_age_days), while this Java record uses
 * Java's camelCase (planTier, accountAgeDays) - the annotations bridge that without
 * either side having to compromise its own language's conventions.
 */
@Component
public class MlModelPriorityAdapter implements PriorityPredictionPort {

    private record PredictRequest(
            String subject,
            String body,
            @JsonProperty("plan_tier") String planTier,
            @JsonProperty("account_age_days") int accountAgeDays,
            @JsonProperty("prior_ticket_count_30d") int priorTicketCount30d) {
    }

    private record PredictResponse(
            String priority,
            double confidence,
            @JsonProperty("class_probabilities") Map<String, Double> classProbabilities) {
    }

    private final RestClient mlServiceClient;

    public MlModelPriorityAdapter(@Qualifier("mlServiceClient") RestClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    @Override
    public PriorityPrediction predict(String subject, String body, String planTier, int accountAgeDays, int priorTicketCount30d) {
        PredictResponse response = mlServiceClient.post()
                .uri("/predict")
                .body(new PredictRequest(subject, body, planTier, accountAgeDays, priorTicketCount30d))
                .retrieve()
                .body(PredictResponse.class);

        return new PriorityPrediction(response.priority(), response.confidence(), response.classProbabilities());
    }
}
