package com.acmesupport.cleanarch.domain.policy;

import com.acmesupport.cleanarch.domain.model.TicketClassification;

/**
 * A genuine Enterprise Business Rule in Uncle Bob's sense: given a classification,
 * should this ticket go to a human? This has nothing to do with AI, HTTP, or
 * persistence - it's pure policy, decided the same way whether the classification came
 * from an LLM, a rules engine, or a person - which is exactly why it belongs in the
 * innermost layer rather than inside a use case or an adapter. It's also trivially
 * unit-testable (see TriageTicketServiceTest) with no framework or mock in sight.
 */
public final class EscalationPolicy {

    private EscalationPolicy() {
    }

    public static boolean requiresHumanEscalation(TicketClassification classification) {
        boolean highUrgency = classification.priority() == TicketClassification.Priority.URGENT
                || classification.priority() == TicketClassification.Priority.HIGH;
        boolean negativeTone = classification.sentiment() == TicketClassification.Sentiment.ANGRY
                || classification.sentiment() == TicketClassification.Sentiment.NEGATIVE;
        return highUrgency && negativeTone;
    }
}
