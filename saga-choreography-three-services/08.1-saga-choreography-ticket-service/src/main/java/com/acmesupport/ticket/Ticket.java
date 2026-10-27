package com.acmesupport.ticket;

public record Ticket(String id, String customerId, String customerName, String subject, Status status) {

    public enum Status {
        OPEN, REFUND_IN_PROGRESS, RESOLVED, REFUND_DECLINED
    }

    public Ticket withStatus(Status newStatus) {
        return new Ticket(id, customerId, customerName, subject, newStatus);
    }
}
