package com.acmesupport.orchestrator.saga;

import com.acmesupport.orchestrator.saga.steps.ConfirmTicketResolvedStep;
import com.acmesupport.orchestrator.saga.steps.HoldTicketStep;
import com.acmesupport.orchestrator.saga.steps.ProcessRefundStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefundSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RefundSagaOrchestrator.class);

    public record SagaExecutionRecord(
            String sagaId,
            String ticketId,
            RefundSagaContext.Outcome outcome,
            List<String> completedSteps,
            List<String> compensatedSteps,
            String customerMessage) {
    }

    private record CommunicationRequest(
            String ticketId, String customerId, String customerName, String outcome, long amountCents, String reason) {
    }

    private record CommunicationResponse(String message) {
    }

    private final List<SagaStep<RefundSagaContext>> steps;
    private final RestClient communicationServiceClient;
    private final Map<String, SagaExecutionRecord> sagaLog = new ConcurrentHashMap<>();

    public RefundSagaOrchestrator(
            HoldTicketStep holdTicketStep,
            ProcessRefundStep processRefundStep,
            ConfirmTicketResolvedStep confirmTicketResolvedStep,
            @Qualifier("communicationServiceClient") RestClient communicationServiceClient) {
        // The saga's shape, in one place: this exact list, in this exact order, IS
        // the business transaction. Reordering these two lines would change what the
        // saga means - which is precisely the kind of thing orchestration makes
        // visible and choreography tends to hide across scattered event handlers.
        this.steps = List.of(holdTicketStep, processRefundStep, confirmTicketResolvedStep);
        this.communicationServiceClient = communicationServiceClient;
    }

    public SagaExecutionRecord run(String ticketId, long amountCents, boolean simulateFailureAfterBilling) {
        String sagaId = UUID.randomUUID().toString();
        RefundSagaContext ctx = new RefundSagaContext(ticketId, amountCents, simulateFailureAfterBilling);

        List<SagaStep<RefundSagaContext>> completed = new ArrayList<>();
        List<String> compensated = new ArrayList<>();
        RefundSagaContext.Outcome outcome;
        String declineReason = null;

        try {
            for (SagaStep<RefundSagaContext> step : steps) {
                log.info("[{}] executing step: {}", sagaId, step.name());
                step.execute(ctx);
                completed.add(step);
            }
            outcome = RefundSagaContext.Outcome.APPROVED;

        } catch (RuntimeException failure) {
            declineReason = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
            log.warn("[{}] step failed ({}) - compensating {} completed step(s)", sagaId, declineReason, completed.size());

            List<SagaStep<RefundSagaContext>> toCompensate = new ArrayList<>(completed);
            Collections.reverse(toCompensate);
            for (SagaStep<RefundSagaContext> step : toCompensate) {
                try {
                    step.compensate(ctx);
                    compensated.add(step.name());
                    log.info("[{}] compensated step: {}", sagaId, step.name());
                } catch (RuntimeException compensationFailure) {
                    // A failed compensation is the scenario sagas cannot fully protect
                    // against on their own: it needs alerting and manual intervention
                    // in a real system (a dead-letter queue, an on-call page), not
                    // silent continuation. Logged loudly here rather than swallowed.
                    log.error("[{}] COMPENSATION FAILED for step {}: {}", sagaId, step.name(), compensationFailure.getMessage());
                }
            }
            outcome = RefundSagaContext.Outcome.DECLINED;
        }

        String customerMessage = draftAndDeliverOutcome(ctx, outcome, declineReason);

        SagaExecutionRecord record = new SagaExecutionRecord(
                sagaId,
                ticketId,
                outcome,
                completed.stream().map(SagaStep::name).toList(),
                compensated,
                customerMessage);
        sagaLog.put(sagaId, record);
        return record;
    }

    public Optional<SagaExecutionRecord> find(String sagaId) {
        return Optional.ofNullable(sagaLog.get(sagaId));
    }

    /**
     * Not part of the compensatable chain - called unconditionally, once, after the
     * saga has already resolved one way or the other. See CommunicationServiceApplication's
     * javadoc for why drafting a message doesn't need a compensating action.
     */
    private String draftAndDeliverOutcome(RefundSagaContext ctx, RefundSagaContext.Outcome outcome, String declineReason) {
        CommunicationResponse response = communicationServiceClient.post()
                .uri("/internal/communications/refund-outcome")
                .body(new CommunicationRequest(
                        ctx.ticketId(),
                        ctx.customerId(),
                        ctx.customerName() != null ? ctx.customerName() : "there",
                        outcome.name(),
                        ctx.amountCents(),
                        declineReason))
                .retrieve()
                .body(CommunicationResponse.class);
        return response.message();
    }
}
