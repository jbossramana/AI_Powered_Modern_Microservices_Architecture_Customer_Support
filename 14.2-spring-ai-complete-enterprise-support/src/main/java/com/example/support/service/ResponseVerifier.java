package com.example.support.service;

import com.example.support.model.TicketAnalysis;
import org.springframework.stereotype.Service;

@Service
public class ResponseVerifier {

    public TicketAnalysis verify(TicketAnalysis analysis, String customerMessage) {
        String category = safe(analysis.category(), "OTHER");
        String priority = safe(analysis.priority(), "MEDIUM").toUpperCase();
        String sentiment = safe(analysis.sentiment(), "NEUTRAL").toUpperCase();

        if (!priority.matches("LOW|MEDIUM|HIGH|CRITICAL")) {
            priority = "MEDIUM";
        }
        if (!sentiment.matches("POSITIVE|NEUTRAL|NEGATIVE")) {
            sentiment = "NEUTRAL";
        }

        boolean highRisk = priority.equals("HIGH")
                || priority.equals("CRITICAL")
                || customerMessage.toLowerCase().contains("duplicate charge")
                || customerMessage.toLowerCase().contains("security incident");

        return new TicketAnalysis(
                category,
                priority,
                sentiment,
                safe(analysis.summary(), "Summary unavailable."),
                safe(analysis.suggestedResponse(), "A support agent will review your request."),
                safe(analysis.recommendedAction(), "Review the ticket and verify the relevant records."),
                analysis.humanReviewRequired() || highRisk
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
