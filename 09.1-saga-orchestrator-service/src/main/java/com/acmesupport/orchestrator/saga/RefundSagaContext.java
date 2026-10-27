package com.acmesupport.orchestrator.saga;

/**
 * Mutable state threaded through every step of one saga run. Steps read what earlier
 * steps produced (ProcessRefundStep needs the customerId HoldTicketStep looked up)
 * and write what later steps - or their own compensate() if something downstream
 * fails - will need.
 */
public class RefundSagaContext {

    public enum Outcome {
        APPROVED, DECLINED
    }

    private final String ticketId;
    private final long amountCents;
    private final boolean simulateFailureAfterBilling;

    private String customerId;
    private String customerName;
    private String refundId;

    public RefundSagaContext(String ticketId, long amountCents, boolean simulateFailureAfterBilling) {
        this.ticketId = ticketId;
        this.amountCents = amountCents;
        this.simulateFailureAfterBilling = simulateFailureAfterBilling;
    }

    public String ticketId() {
        return ticketId;
    }

    public long amountCents() {
        return amountCents;
    }

    public boolean simulateFailureAfterBilling() {
        return simulateFailureAfterBilling;
    }

    public String customerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String customerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /** Set once ProcessRefundStep succeeds; null until then, and null again is meaningless after - it's what compensate() checks to know whether there's anything to reverse. */
    public String refundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
}
