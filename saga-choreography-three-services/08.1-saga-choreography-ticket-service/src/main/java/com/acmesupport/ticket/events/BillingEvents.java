package com.acmesupport.ticket.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Ticket-service's OWN view of the events billing-service publishes - a deliberately
 * independent copy rather than a shared "event contracts" library dependency. Real
 * teams debate this tradeoff: a shared JAR keeps producer and consumer definitions in
 * sync automatically but recouples services at build time - exactly what choreography
 * is trying to avoid in the first place; independent copies like these keep services
 * deployable on their own schedules but mean each consumer must notice and adopt a
 * producer's schema changes itself. This module takes the independent-copy side; see
 * the top-level README for the full discussion.
 */
public final class BillingEvents {

    private BillingEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundProcessed(String type, String sagaId, String ticketId, String customerId, String refundId, long amountCents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundDeclined(String type, String sagaId, String ticketId, String customerId, long amountCents, String reason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundReversed(String type, String sagaId, String ticketId, String refundId, long amountCents) {
    }
}
