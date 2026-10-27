package com.acmesupport.hitl;

import java.time.Instant;

/**
 * An immutable snapshot of one AI-drafted reply's review lifecycle. Each transition
 * method returns a new instance rather than mutating in place - the service stores
 * whichever is current in a map, so there's never ambiguity about which version of a
 * draft is "the real one" while a review is happening.
 */
public record PendingApproval(
        String id,
        String ticketId,
        String customerId,
        String draftResponse,
        Status status,
        String reviewedBy,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt) {

    public enum Status {
        PENDING, APPROVED, REJECTED, SENT
    }

    static PendingApproval newDraft(String id, String ticketId, String customerId, String draftResponse) {
        return new PendingApproval(id, ticketId, customerId, draftResponse, Status.PENDING, null, null, Instant.now(), null);
    }

    /**
     * @param finalText the text to actually send - either the original draft (reviewer
     *                  agreed with it) or a reviewer-edited version.
     */
    PendingApproval approve(String reviewer, String finalText) {
        return new PendingApproval(id, ticketId, customerId, finalText, Status.APPROVED, reviewer, null, createdAt, Instant.now());
    }

    PendingApproval reject(String reviewer, String reason) {
        return new PendingApproval(id, ticketId, customerId, draftResponse, Status.REJECTED, reviewer, reason, createdAt, Instant.now());
    }

    PendingApproval markSent() {
        return new PendingApproval(id, ticketId, customerId, draftResponse, Status.SENT, reviewedBy, reviewNote, createdAt, Instant.now());
    }
}
