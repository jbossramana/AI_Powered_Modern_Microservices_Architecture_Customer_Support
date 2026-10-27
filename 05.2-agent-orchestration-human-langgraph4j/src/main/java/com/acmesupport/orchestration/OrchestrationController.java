package com.acmesupport.orchestration;

import com.acmesupport.orchestration.graph.SupportGraphState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class OrchestrationController {
    private final CompiledGraph<SupportGraphState> supportGraph;
    private final ApprovalQueue approvalQueue;

    public OrchestrationController(CompiledGraph<SupportGraphState> supportGraph, ApprovalQueue approvalQueue) {
        this.supportGraph = supportGraph;
        this.approvalQueue = approvalQueue;
    }

    public record ProcessTicketRequest(@NotBlank String ticketId, @NotBlank String customerId,
                                       @NotBlank String ticketText) {}
    public record ApprovalDecisionRequest(@NotNull ApprovalQueue.Status decision,
                                          @NotBlank String reviewer, String comment) {}
    public record TicketOutcome(String ticketId, String customerId, String category, String priority,
                                String sentiment, String summary, String retrievedContext,
                                String draftResponse, String escalationReason, String route,
                                String approvalStatus, String message) {}

    @PostMapping("/process")
    public TicketOutcome process(@Valid @RequestBody ProcessTicketRequest request) throws Exception {
        Map<String, Object> initialState = Map.of(
                SupportGraphState.TICKET_ID, request.ticketId(),
                SupportGraphState.CUSTOMER_ID, request.customerId(),
                SupportGraphState.TICKET_TEXT, request.ticketText());
        SupportGraphState result = supportGraph.invoke(initialState)
                .orElseThrow(() -> new IllegalStateException("Graph produced no final state"));

        String actionType = result.reviewType().isBlank()
                ? (result.escalationReason().isBlank() ? "RESPONSE_DRAFT" : "ESCALATION")
                : result.reviewType();
        ApprovalQueue.ApprovalItem item;
        try {
            item = approvalQueue.submit(result.ticketId(), result.customerId(), actionType,
                    result.ticketText(), result.draftResponse(), result.escalationReason(),
                    result.category(), result.priority(), result.sentiment(), result.summary());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
        String route = "ESCALATION".equals(actionType)
                ? "AWAITING_HUMAN_ESCALATION_REVIEW" : "AWAITING_HUMAN_APPROVAL";
        String message = "ESCALATION".equals(actionType)
                ? "Escalation saved for human review. No customer message was sent."
                : "Draft saved for human approval. It must not be sent to the customer until approved.";
        return new TicketOutcome(result.ticketId(), result.customerId(), result.category(), result.priority(),
                result.sentiment(), result.summary(), result.retrievedContext(), result.draftResponse(),
                result.escalationReason(), route, item.approvalStatus().name(), message);
    }

    @GetMapping("/approvals/pending")
    public java.util.List<ApprovalQueue.ApprovalItem> pendingApprovals() {
        return approvalQueue.pending();
    }

    @GetMapping("/{ticketId}/approval")
    public ApprovalQueue.ApprovalItem getApproval(@PathVariable String ticketId) {
        try { return approvalQueue.get(ticketId); }
        catch (ApprovalQueue.ApprovalNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{ticketId}/approval")
    public ApprovalQueue.ApprovalItem decideApproval(@PathVariable String ticketId,
                                                      @Valid @RequestBody ApprovalDecisionRequest request) {
        try {
            return approvalQueue.decide(ticketId, request.decision(), request.reviewer(), request.comment());
        } catch (ApprovalQueue.ApprovalNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }
}
