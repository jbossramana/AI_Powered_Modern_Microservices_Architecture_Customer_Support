package com.acmesupport.orchestrator.saga;

/**
 * For steps that need to fail based on their own logic - a downstream participant's
 * error response translated into a clean message (see {@code RemoteErrors}), or the
 * test-only {@code simulateFailureAfterBilling} hook in {@code ConfirmTicketResolvedStep}
 * - rather than an unexamined exception bubbling up from a failed HTTP call. The
 * orchestrator treats this identically to any other RuntimeException from a step: both
 * trigger compensation of whatever already completed.
 */
public class SagaStepException extends RuntimeException {
    public SagaStepException(String message) {
        super(message);
    }
}
