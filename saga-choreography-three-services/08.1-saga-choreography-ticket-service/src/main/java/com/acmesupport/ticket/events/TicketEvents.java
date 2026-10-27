package com.acmesupport.ticket.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Every event carries a "type" field so a listener on a shared topic can tell events
 * apart before deserializing into a specific shape, and a "sagaId" so events scattered
 * across topics and consumers can be correlated back to one business transaction -
 * there's no orchestrator to do that correlation centrally anymore.
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} on every event: a resilience
 * habit worth having from the start in event-driven systems, where a producer adding a
 * field later shouldn't break every existing consumer.
 */
public final class TicketEvents {

    private TicketEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketRefundHeld(
            String type, String sagaId, String ticketId, String customerId, String customerName,
            long amountCents, boolean simulateFailureAfterBilling) {

        public TicketRefundHeld(String sagaId, String ticketId, String customerId, String customerName,
                long amountCents, boolean simulateFailureAfterBilling) {
            this("TicketRefundHeld", sagaId, ticketId, customerId, customerName, amountCents, simulateFailureAfterBilling);
        }
    }

    /** Published instead of resolving, when the simulateFailureAfterBilling test hook fires. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketResolutionFailed(String type, String sagaId, String ticketId, String refundId, String reason) {
        public TicketResolutionFailed(String sagaId, String ticketId, String refundId, String reason) {
            this("TicketResolutionFailed", sagaId, ticketId, refundId, reason);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketRefundResolved(String type, String sagaId, String ticketId, String customerId, String customerName, long amountCents) {
        public TicketRefundResolved(String sagaId, String ticketId, String customerId, String customerName, long amountCents) {
            this("TicketRefundResolved", sagaId, ticketId, customerId, customerName, amountCents);
        }
    }

    /** Published both for a natural decline (billing rejected) and after a reversal (compensation completed). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketRefundDeclined(String type, String sagaId, String ticketId, String customerId, String customerName, long amountCents, String reason) {
        public TicketRefundDeclined(String sagaId, String ticketId, String customerId, String customerName, long amountCents, String reason) {
            this("TicketRefundDeclined", sagaId, ticketId, customerId, customerName, amountCents, reason);
        }
    }

    /** The fan-in event: the only thing communication-service ever listens to. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundOutcomeReady(
            String type, String sagaId, String ticketId, String customerId, String customerName,
            String outcome, long amountCents, String reason) {

        public RefundOutcomeReady(String sagaId, String ticketId, String customerId, String customerName,
                String outcome, long amountCents, String reason) {
            this("RefundOutcomeReady", sagaId, ticketId, customerId, customerName, outcome, amountCents, reason);
        }
    }
}
