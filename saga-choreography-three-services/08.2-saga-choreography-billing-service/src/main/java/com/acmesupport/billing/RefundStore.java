package com.acmesupport.billing;

import com.acmesupport.billing.policy.RefundPolicy;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RefundStore {

    private final Map<String, Refund> refunds = new ConcurrentHashMap<>();

    public Refund process(String sagaId, String ticketId, String customerId, long amountCents) {
        RefundPolicy.validate(amountCents);
        Refund refund = new Refund(UUID.randomUUID().toString(), sagaId, ticketId, customerId, amountCents, Refund.Status.COMPLETED);
        refunds.put(refund.id(), refund);
        return refund;
    }

    public Refund reverse(String refundId) {
        Refund current = find(refundId).orElseThrow(() -> new RefundNotFoundException(refundId));
        Refund reversed = current.reversed();
        refunds.put(refundId, reversed);
        return reversed;
    }

    public Optional<Refund> find(String refundId) {
        return Optional.ofNullable(refunds.get(refundId));
    }
}
