package com.acmesupport.billing.policy;

import com.acmesupport.billing.RefundPolicyViolationException;

/**
 * Identical rule to the orchestration version's RefundPolicy - the business logic
 * doesn't change based on how a request arrives (REST call vs. Kafka event). That's
 * worth noticing: only the transport and the failure-signaling mechanism differ
 * between the two versions of this saga, not the domain rules themselves.
 */
public final class RefundPolicy {

    public static final long MAX_AUTOMATIC_REFUND_CENTS = 10_000; // $100.00

    private RefundPolicy() {
    }

    public static void validate(long amountCents) {
        if (amountCents <= 0) {
            throw new RefundPolicyViolationException("Refund amount must be greater than zero.");
        }
        if (amountCents > MAX_AUTOMATIC_REFUND_CENTS) {
            throw new RefundPolicyViolationException(
                    "Refund amount of $%.2f exceeds the $%.2f automatic-approval limit and needs manual review."
                            .formatted(amountCents / 100.0, MAX_AUTOMATIC_REFUND_CENTS / 100.0));
        }
    }
}
