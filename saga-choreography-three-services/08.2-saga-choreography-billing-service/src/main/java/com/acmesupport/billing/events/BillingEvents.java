package com.acmesupport.billing.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public final class BillingEvents {

    private BillingEvents() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundProcessed(String type, String sagaId, String ticketId, String customerId, String refundId, long amountCents) {
        public RefundProcessed(String sagaId, String ticketId, String customerId, String refundId, long amountCents) {
            this("RefundProcessed", sagaId, ticketId, customerId, refundId, amountCents);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundDeclined(String type, String sagaId, String ticketId, String customerId, long amountCents, String reason) {
        public RefundDeclined(String sagaId, String ticketId, String customerId, long amountCents, String reason) {
            this("RefundDeclined", sagaId, ticketId, customerId, amountCents, reason);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundReversed(String type, String sagaId, String ticketId, String refundId, long amountCents) {
        public RefundReversed(String sagaId, String ticketId, String refundId, long amountCents) {
            this("RefundReversed", sagaId, ticketId, refundId, amountCents);
        }
    }
}
