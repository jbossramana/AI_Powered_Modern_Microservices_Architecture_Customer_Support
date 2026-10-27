package com.acmesupport.communication.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * communication-service's own view of ticket-service's fan-in event - the only
 * event type this service cares about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefundOutcomeReady(String type, String sagaId, String ticketId, String customerId, String customerName,
		String outcome, long amountCents, String reason) {
}
