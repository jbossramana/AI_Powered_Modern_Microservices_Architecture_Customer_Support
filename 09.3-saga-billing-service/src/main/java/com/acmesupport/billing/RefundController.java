package com.acmesupport.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/refunds")
public class RefundController {

    private final RefundStore store;

    public RefundController(RefundStore store) {
        this.store = store;
    }

    public record RefundRequest(@NotBlank String ticketId, @NotBlank String customerId, @Positive long amountCents) {
    }

    @GetMapping("/{id}")
    public Refund get(@PathVariable String id) {
        return store.find(id).orElseThrow(() -> new RefundNotFoundException(id));
    }

    /** Forward action, saga step 2. Returns 422 if RefundPolicy rejects the amount. */
    @PostMapping
    public Refund process(@RequestBody RefundRequest request) {
        return store.process(request.ticketId(), request.customerId(), request.amountCents());
    }

    /** Compensating action for a completed refund. */
    @PostMapping("/{id}/reverse")
    public Refund reverse(@PathVariable String id) {
        return store.reverse(id);
    }

    @ExceptionHandler(RefundPolicyViolationException.class)
    public ResponseEntity<Map<String, String>> handlePolicyViolation(RefundPolicyViolationException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RefundNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RefundNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
