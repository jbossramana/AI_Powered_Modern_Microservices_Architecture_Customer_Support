package com.acmesupport.hitl;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class ApprovalController {

    private final DraftAndApprovalService service;

    public ApprovalController(DraftAndApprovalService service) {
        this.service = service;
    }

    public record DraftRequest(@NotBlank String ticketId, @NotBlank String customerId, @NotBlank String ticketText) {
    }

    public record ApproveRequest(@NotBlank String reviewer, String editedText) {
    }

    public record RejectRequest(@NotBlank String reviewer, @NotBlank String reason) {
    }

    /** Step 1: draft a reply. Returns immediately with status PENDING - nothing is sent. */
    @PostMapping("/tickets/draft")
    public PendingApproval draft(@RequestBody DraftRequest request) {
        return service.draftForReview(request.ticketId(), request.customerId(), request.ticketText());
    }

    /** The support engineer's queue. Defaults to PENDING items needing review. */
    @GetMapping("/approvals")
    public List<PendingApproval> list(
            @RequestParam(defaultValue = "PENDING") PendingApproval.Status status) {
        return service.listByStatus(status);
    }

    @GetMapping("/approvals/{id}")
    public PendingApproval getOne(@PathVariable String id) {
        return service.get(id);
    }

    /** Approve as-is (omit editedText) or approve with edits (provide editedText). */
    @PostMapping("/approvals/{id}/approve")
    public PendingApproval approve(@PathVariable String id, @RequestBody ApproveRequest request) {
        return service.approve(id, request.reviewer(), request.editedText());
    }

    @PostMapping("/approvals/{id}/reject")
    public PendingApproval reject(@PathVariable String id, @RequestBody RejectRequest request) {
        return service.reject(id, request.reviewer(), request.reason());
    }

    /** Only succeeds if the item's status is APPROVED - see DraftAndApprovalService.send(). */
    @PostMapping("/approvals/{id}/send")
    public PendingApproval send(@PathVariable String id) {
        return service.send(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
