package com.example.support.model;

public record SupportReply(
        String answer,
        String category,
        String priority,
        String sentiment,
        boolean humanReviewRequired,
        String[] sources
) {
}
