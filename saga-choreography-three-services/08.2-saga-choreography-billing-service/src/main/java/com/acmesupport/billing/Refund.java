package com.acmesupport.billing;

public record Refund(String id, String sagaId, String ticketId, String customerId, long amountCents, Status status) {

    public enum Status {
        COMPLETED, REVERSED
    }

    public Refund reversed() {
        return new Refund(id, sagaId, ticketId, customerId, amountCents, Status.REVERSED);
    }
}
