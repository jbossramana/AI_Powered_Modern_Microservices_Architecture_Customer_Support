package com.example.support.model;

public record SupportTicket(
        String ticketId,
        String category,
        String priority,
        String sentiment,
        String summary,
        String suggestedResponse
) {}
