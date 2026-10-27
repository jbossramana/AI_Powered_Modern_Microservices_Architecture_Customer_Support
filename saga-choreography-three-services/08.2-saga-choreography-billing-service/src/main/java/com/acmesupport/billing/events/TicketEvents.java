package com.acmesupport.billing.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** See ticket-service's BillingEvents.java for the full discussion of why this is an independent copy, not a shared library. */
public final class TicketEvents {

    private TicketEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketRefundHeld(
            String type, String sagaId, String ticketId, String customerId, String customerName,
            long amountCents, boolean simulateFailureAfterBilling) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketResolutionFailed(String type, String sagaId, String ticketId, String refundId, String reason) {
    }
}
