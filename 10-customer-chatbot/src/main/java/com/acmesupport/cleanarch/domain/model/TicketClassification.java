package com.acmesupport.cleanarch.domain.model;

/**
 * A value object: the result of judging a ticket. Nothing about category, priority,
 * sentiment, or summary implies "an LLM produced this" - it could just as easily come
 * from a rules engine or a person filling out a form. That's deliberate: the domain
 * layer describes WHAT a classification is, never HOW one gets produced.
 */
public record TicketClassification(Category category, Priority priority, Sentiment sentiment, String summary) {

    public enum Category {
        ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, FEATURE_REQUEST, OTHER
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE, ANGRY
    }
}
