package com.example.support.model;

public record TicketAnalysis(
        String category,
        String priority,
        String sentiment,
        String summary,
        String suggestedResponse,
        String recommendedAction,
        boolean humanReviewRequired
) {
}
