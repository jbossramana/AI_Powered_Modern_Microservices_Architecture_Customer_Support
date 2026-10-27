package com.acmesupport.cleanarch.application.port.in;

import com.acmesupport.cleanarch.domain.model.TicketClassification;

/**
 * An inbound port: what the outside world (adapter/in/web/TicketController, or a test)
 * can ask this application to do. This interface, its Command/Result records, and
 * everything it references live in domain/ or application/ - no Spring, no Spring AI.
 */
public interface TriageTicketUseCase {

    TriageResult triage(TriageCommand command);

    record TriageCommand(String customerId, String ticketText) {
    }

    record TriageResult(TicketClassification classification, boolean requiresHumanEscalation) {
    }
}
