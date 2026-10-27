package com.acmesupport.billing.policy;

import com.acmesupport.billing.RefundPolicyViolationException;

/**
 * The rule that gives this saga a realistic, deterministic failure to demonstrate:
 * request more than the automatic-approval limit and billing declines, no randomness
 * needed. A second, independent way to see a failure - one that happens further into
 * the saga, after money has already moved - is the orchestrator's
 * simulateFailureAfterBilling test flag; see saga-orchestrator-service for that one.
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
