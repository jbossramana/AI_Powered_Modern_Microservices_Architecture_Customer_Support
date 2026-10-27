package com.acmesupport.cleanarch.domain.model;

import java.time.Instant;

/**
 * An entity with a lifecycle (PENDING -> APPROVED/REJECTED -> SENT), same idea as
 * PendingApproval in 06-human-in-the-loop-spring-ai. Each transition method returns a
 * new instance; the calling use case decides whether/how to persist it via
 * DraftResponseRepositoryPort. The rule "you can't send anything but an APPROVED draft"
 * lives in the use case (DraftResponseService), not here - this class only knows how
 * to compute what the next state looks like, not when a transition is allowed.
 */
public record DraftResponse(
        String id,
        String ticketId,
        String bodyText,
        ApprovalStatus status,
        String reviewedBy,
        String reviewNote,
        Instant createdAt,
        Instant reviewedAt) {

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED, SENT
    }

    public static DraftResponse newDraft(String id, String ticketId, String bodyText) {
        return new DraftResponse(id, ticketId, bodyText, ApprovalStatus.PENDING, null, null, Instant.now(), null);
    }

    public DraftResponse approve(String reviewer, String finalText) {
        return new DraftResponse(id, ticketId, finalText, ApprovalStatus.APPROVED, reviewer, null, createdAt, Instant.now());
    }

    public DraftResponse reject(String reviewer, String reason) {
        return new DraftResponse(id, ticketId, bodyText, ApprovalStatus.REJECTED, reviewer, reason, createdAt, Instant.now());
    }

    public DraftResponse markSent() {
        return new DraftResponse(id, ticketId, bodyText, ApprovalStatus.SENT, reviewedBy, reviewNote, createdAt, Instant.now());
    }
}
