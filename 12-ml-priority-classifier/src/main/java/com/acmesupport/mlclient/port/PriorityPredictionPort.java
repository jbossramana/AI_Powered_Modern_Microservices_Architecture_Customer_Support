package com.acmesupport.mlclient.port;

import java.util.Map;

/**
 * Deliberately narrower than clean-architecture-spring-ai's TicketClassifierPort
 * (category + priority + sentiment + summary in one call). This model was trained to
 * do ONE thing - predict priority from text and account signals - and the port matches
 * that scope exactly rather than forcing a single interface to cover capabilities the
 * underlying model doesn't actually have. A real system would likely keep category and
 * sentiment on the LLM-backed port and use this one just for priority, calling both
 * where needed - not every prediction in the same business process has to come from
 * the same kind of model.
 */
public interface PriorityPredictionPort {

    PriorityPrediction predict(String subject, String body, String planTier, int accountAgeDays, int priorTicketCount30d);

    record PriorityPrediction(String priority, double confidence, Map<String, Double> classProbabilities) {
    }
}
