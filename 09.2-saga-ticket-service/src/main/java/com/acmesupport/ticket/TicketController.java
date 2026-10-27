package com.acmesupport.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/tickets")
public class TicketController {

    private final TicketStore store;

    public TicketController(TicketStore store) {
        this.store = store;
    }

    @GetMapping("/{id}")
    public Ticket get(@PathVariable String id) {
        return store.find(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    /** Forward action, saga step 1. Called by the orchestrator before touching billing. */
    @PostMapping("/{id}/refund-hold")
    public Ticket hold(@PathVariable String id) {
        return store.hold(id);
    }

    /** Compensating action for /refund-hold. Called by the orchestrator if a later step fails. */
    @PostMapping("/{id}/refund-hold/release")
    public Ticket releaseHold(@PathVariable String id) {
        return store.releaseHold(id);
    }

    /** Forward action, saga's final confirming step. */
    @PostMapping("/{id}/resolve")
    public Ticket resolve(@PathVariable String id) {
        return store.resolve(id);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTicketStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(InvalidTicketStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
