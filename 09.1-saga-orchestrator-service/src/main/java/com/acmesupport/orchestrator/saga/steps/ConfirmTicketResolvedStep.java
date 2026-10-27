package com.acmesupport.orchestrator.saga.steps;

import com.acmesupport.orchestrator.saga.RefundSagaContext;
import com.acmesupport.orchestrator.saga.SagaStep;
import com.acmesupport.orchestrator.saga.SagaStepException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * The saga's final compensatable step: confirm the ticket as resolved now that the
 * refund actually completed. Its execute() also honors a TEST-ONLY hook,
 * simulateFailureAfterBilling, which deliberately fails here instead of calling
 * ticket-service - the only deterministic way to exercise the "compensate a step that
 * already moved money" path in this demo, since there's no natural business rule that
 * makes confirmation itself fail.
 */
@Component
public class ConfirmTicketResolvedStep implements SagaStep<RefundSagaContext> {

    private final RestClient ticketServiceClient;
    private final ObjectMapper objectMapper;

    public ConfirmTicketResolvedStep(@Qualifier("ticketServiceClient") RestClient ticketServiceClient, ObjectMapper objectMapper) {
        this.ticketServiceClient = ticketServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "confirm-ticket-resolved";
    }

    @Override
    public void execute(RefundSagaContext ctx) {
        if (ctx.simulateFailureAfterBilling()) {
            throw new SagaStepException(
                    "Simulated failure after billing already succeeded (test-only hook: simulateFailureAfterBilling=true).");
        }
        try {
            ticketServiceClient.post()
                    .uri("/internal/tickets/{id}/resolve", ctx.ticketId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new SagaStepException(RemoteErrors.extract(e, objectMapper, "The ticket could not be marked resolved."));
        }
    }

    @Override
    public void compensate(RefundSagaContext ctx) {
        // Intentionally a no-op: this step either fully succeeded (the ticket is
        // RESOLVED, a terminal state - nothing needs undoing since the whole saga
        // succeeded) or it never took effect at all. What actually undoes a failure
        // HERE is compensating the two PRECEDING steps (reverse the refund, release
        // the ticket hold) - not anything this step itself needs to reverse. A saga
        // step's compensate() only needs to undo what its OWN execute() did.
    }
}
