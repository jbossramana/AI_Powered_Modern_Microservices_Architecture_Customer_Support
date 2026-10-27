package com.acmesupport.orchestration;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Demo in-memory human review queue. Replace with durable storage in production. */
@Service
public class ApprovalQueue {
    public enum Status { PENDING, APPROVED, REJECTED }

    /** actionType is RESPONSE_DRAFT or ESCALATION. APPROVED means approved for the
     * corresponding next step; it does not itself send a customer message. */
    public record ApprovalItem(
            String ticketId, String customerId, String actionType, String ticketText,
            String draftResponse, String escalationReason, String category, String priority,
            String sentiment, String summary, Status approvalStatus, String reviewer,
            String reviewerComment, Instant createdAt, Instant decidedAt) {
        ApprovalItem decide(Status status, String reviewer, String comment) {
            return new ApprovalItem(ticketId, customerId, actionType, ticketText, draftResponse,
                    escalationReason, category, priority, sentiment, summary, status, reviewer,
                    comment, createdAt, Instant.now());
        }
    }

    private final Map<String, ApprovalItem> items = new ConcurrentHashMap<>();

    public ApprovalItem submit(String ticketId, String customerId, String actionType, String ticketText,
                               String draft, String escalationReason, String category, String priority,
                               String sentiment, String summary) {
        ApprovalItem item = new ApprovalItem(ticketId, customerId, actionType, ticketText, draft,
                escalationReason, category, priority, sentiment, summary, Status.PENDING, "", "",
                Instant.now(), null);
        ApprovalItem previous = items.putIfAbsent(ticketId, item);
        if (previous != null && previous.approvalStatus() == Status.PENDING) {
            throw new IllegalStateException("Ticket already has a pending approval: " + ticketId);
        }
        if (previous != null) items.replace(ticketId, previous, item);
        return item;
    }

    public ApprovalItem decide(String ticketId, Status decision, String reviewer, String comment) {
        if (decision == Status.PENDING) throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        if (reviewer == null || reviewer.isBlank()) throw new IllegalArgumentException("Reviewer is required");
        return items.compute(ticketId, (id, current) -> {
            if (current == null) throw new ApprovalNotFoundException(id);
            if (current.approvalStatus() != Status.PENDING)
                throw new IllegalStateException("Approval already decided: " + current.approvalStatus());
            return current.decide(decision, reviewer, comment == null ? "" : comment);
        });
    }

    public ApprovalItem get(String ticketId) {
        ApprovalItem item = items.get(ticketId);
        if (item == null) throw new ApprovalNotFoundException(ticketId);
        return item;
    }

    public List<ApprovalItem> pending() {
        return items.values().stream().filter(i -> i.approvalStatus() == Status.PENDING)
                .sorted(Comparator.comparing(ApprovalItem::createdAt)).toList();
    }

    public static class ApprovalNotFoundException extends RuntimeException {
        public ApprovalNotFoundException(String ticketId) { super("No approval found for ticket: " + ticketId); }
    }
}
