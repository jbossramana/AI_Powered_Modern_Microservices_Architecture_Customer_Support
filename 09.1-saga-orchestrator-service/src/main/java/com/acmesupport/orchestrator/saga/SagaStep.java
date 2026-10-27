package com.acmesupport.orchestrator.saga;

/**
 * The core saga abstraction: a step knows how to do something ({@link #execute}) and
 * how to undo it ({@link #compensate}). {@link RefundSagaOrchestrator} only ever calls
 * these two methods - it has no idea what "hold a ticket" or "reverse a refund"
 * actually means underneath, which is what lets each participant service evolve
 * independently as long as it honors this contract.
 *
 * @param <C> the saga's shared context type, carrying whatever data steps produce for
 *            later steps - or for their own compensation - to use.
 */
public interface SagaStep<C> {

    /** A short, stable name used in saga execution records and logs. */
    String name();

    /**
     * Perform this step's local transaction. Any RuntimeException - including
     * {@link SagaStepException} - is treated identically by the orchestrator: it
     * stops the forward chain and triggers compensation of every step that already
     * succeeded, in reverse order.
     */
    void execute(C context);

    /**
     * Undo this step's effect. Only called for steps whose {@link #execute} already
     * succeeded, and only when a later step fails. Must be safe to call even when
     * there's conceptually nothing to undo - see {@code ConfirmTicketResolvedStep} for
     * an example of a deliberately no-op compensation, and why that's correct there.
     */
    void compensate(C context);
}
