package com.acmesupport.ticket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOT a saga log in the orchestration sense - there's no central coordinator here to
 * log for. This just remembers enough about sagas THIS service started to (a) know
 * which ticket a later billing-events message belongs to, (b) recall the
 * simulateFailureAfterBilling test flag when reacting to RefundProcessed, and (c) let
 * a customer poll for a result after the initial 202 Accepted - since, unlike the
 * orchestration version, there's no synchronous response with the final outcome to
 * hand back immediately.
 */
@Component
public class SagaTracker {

    public record SagaState(String sagaId, String ticketId, boolean simulateFailureAfterBilling, String status) {
        SagaState withStatus(String newStatus) {
            return new SagaState(sagaId, ticketId, simulateFailureAfterBilling, newStatus);
        }
    }

    private final Map<String, SagaState> sagas = new ConcurrentHashMap<>();

    public void start(String sagaId, String ticketId, boolean simulateFailureAfterBilling) {
        sagas.put(sagaId, new SagaState(sagaId, ticketId, simulateFailureAfterBilling, "PROCESSING"));
    }

    public Optional<SagaState> find(String sagaId) {
        return Optional.ofNullable(sagas.get(sagaId));
    }

    public void updateStatus(String sagaId, String status) {
        sagas.computeIfPresent(sagaId, (id, state) -> state.withStatus(status));
    }
}
