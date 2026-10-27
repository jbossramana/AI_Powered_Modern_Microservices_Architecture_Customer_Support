package com.acmesupport.ticket;

import com.acmesupport.ticket.events.EventPublisher;
import com.acmesupport.ticket.events.TicketEvents;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
public class TicketController {

    private final TicketStore store;
    private final SagaTracker sagaTracker;
    private final EventPublisher publisher;

    public TicketController(TicketStore store, SagaTracker sagaTracker, EventPublisher publisher) {
        this.store = store;
        this.sagaTracker = sagaTracker;
        this.publisher = publisher;
    }

    public record RefundRequest(@Positive long amountCents, boolean simulateFailureAfterBilling) {
    }

    public record RefundAccepted(String sagaId, String ticketId, String status) {
    }

    @GetMapping("/internal/tickets/{id}")
    public Ticket get(@PathVariable String id) {
        return store.find(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    /**
     * POST /api/tickets/{id}/refund
     * {"amountCents": 999}
     *
     * The saga's entry point. Unlike the orchestration version's synchronous
     * response, this can only acknowledge that the process HAS STARTED - the eventual
     * outcome depends on billing-service and communication-service reacting to events
     * published after this method returns. 202 Accepted, not 200, reflects that
     * honestly: the request was accepted for processing, not completed.
     */
    @PostMapping("/api/tickets/{id}/refund")
    public ResponseEntity<RefundAccepted> requestRefund(@PathVariable String id, @Valid @RequestBody RefundRequest request) {
        Ticket ticket = store.hold(id);
        String sagaId = UUID.randomUUID().toString();
        sagaTracker.start(sagaId, id, request.simulateFailureAfterBilling());

        publisher.publish(id, new TicketEvents.TicketRefundHeld(
                sagaId, id, ticket.customerId(), ticket.customerName(),
                request.amountCents(), request.simulateFailureAfterBilling()));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new RefundAccepted(sagaId, id, "PROCESSING"));
    }

    /** Poll this until status is no longer PROCESSING. The AI-drafted message itself lives in communication-service's log. */
    @GetMapping("/api/tickets/{id}/refund/{sagaId}")
    public SagaTracker.SagaState getRefundStatus(@PathVariable String id, @PathVariable String sagaId) {
        return sagaTracker.find(sagaId).orElseThrow(() -> new NoSuchElementException("No saga found with id " + sagaId));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTicketStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(InvalidTicketStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleSagaNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
