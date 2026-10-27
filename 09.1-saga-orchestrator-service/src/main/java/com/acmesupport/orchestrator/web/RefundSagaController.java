package com.acmesupport.orchestrator.web;

import com.acmesupport.orchestrator.saga.RefundSagaOrchestrator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/refund-requests")
public class RefundSagaController {

    private final RefundSagaOrchestrator orchestrator;

    public RefundSagaController(RefundSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public record RefundSagaRequest(
            @NotBlank String ticketId,
            @Positive long amountCents,
            // Test-only: forces a failure AFTER billing succeeds, to exercise the
            // "reverse a completed refund" compensation path deterministically. See
            // the top-level README for why this exists and how to use it.
            boolean simulateFailureAfterBilling) {
    }

    /**
     * POST /api/refund-requests
     * {"ticketId": "TCK-2001", "amountCents": 999}
     *
     * The single customer-facing entry point. Everything below this line - three
     * services, up to two compensations, one LLM call - is invisible to the caller:
     * they get one response with the outcome and the message to show the customer.
     * Always returns 200; APPROVED vs. DECLINED is a business outcome carried in the
     * body, not an HTTP-layer distinction - the saga itself completed successfully
     * either way.
     */
    @PostMapping
    public RefundSagaOrchestrator.SagaExecutionRecord submit(@RequestBody RefundSagaRequest request) {
        return orchestrator.run(request.ticketId(), request.amountCents(), request.simulateFailureAfterBilling());
    }

    /** Inspect a past saga run - which steps completed, which were compensated, and what the customer was told. */
    @GetMapping("/{sagaId}")
    public RefundSagaOrchestrator.SagaExecutionRecord get(@PathVariable String sagaId) {
        return orchestrator.find(sagaId)
                .orElseThrow(() -> new NoSuchElementException("No saga found with id " + sagaId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
