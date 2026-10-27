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
 * The step with real money on the line. Its execute() is where RefundPolicy's
 * $100 automatic-approval limit (in billing-service) surfaces as a natural failure -
 * request more than that and this step fails here, with nothing yet to compensate but
 * HoldTicketStep. Its compensate() is where the more interesting case lives: reversing
 * a refund that already completed, needed only when a LATER step
 * (ConfirmTicketResolvedStep) fails after the money already moved.
 */
@Component
public class ProcessRefundStep implements SagaStep<RefundSagaContext> {

    private record RefundRequestBody(String ticketId, String customerId, long amountCents) {
    }

    private record RefundView(String id, String ticketId, String customerId, long amountCents, String status) {
    }

    private final RestClient billingServiceClient;
    private final ObjectMapper objectMapper;

    public ProcessRefundStep(@Qualifier("billingServiceClient") RestClient billingServiceClient, ObjectMapper objectMapper) {
        this.billingServiceClient = billingServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "process-refund";
    }

    @Override
    public void execute(RefundSagaContext ctx) {
        try {
            RefundView refund = billingServiceClient.post()
                    .uri("/internal/refunds")
                    .body(new RefundRequestBody(ctx.ticketId(), ctx.customerId(), ctx.amountCents()))
                    .retrieve()
                    .body(RefundView.class);
            ctx.setRefundId(refund.id());
        } catch (RestClientResponseException e) {
            // A 422 here is RefundPolicy declining the amount - RestClient throws
            // automatically for any non-2xx, which is exactly the signal the
            // orchestrator needs to start compensating.
            throw new SagaStepException(RemoteErrors.extract(e, objectMapper, "The refund could not be processed."));
        }
    }

    @Override
    public void compensate(RefundSagaContext ctx) {
        if (ctx.refundId() == null) {
            return; // execute() never got far enough to create a refund - nothing to reverse
        }
        billingServiceClient.post()
                .uri("/internal/refunds/{id}/reverse", ctx.refundId())
                .retrieve()
                .toBodilessEntity();
    }
}
