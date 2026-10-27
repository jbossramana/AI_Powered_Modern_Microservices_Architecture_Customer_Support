package com.acmesupport.billing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Read-only now: the forward action (process a refund) and its compensation (reverse
 * it) both happen in TicketEventListener, triggered by Kafka messages, not by callers
 * hitting an endpoint. This controller exists purely so tests/curl can verify what
 * happened, the same role /internal/tickets/{id} plays for ticket-service.
 */
@RestController
public class RefundController {

    private final RefundStore store;

    public RefundController(RefundStore store) {
        this.store = store;
    }

    @GetMapping("/internal/refunds/{id}")
    public Refund get(@PathVariable String id) {
        return store.find(id).orElseThrow(() -> new RefundNotFoundException(id));
    }

    @ExceptionHandler(RefundNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RefundNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
