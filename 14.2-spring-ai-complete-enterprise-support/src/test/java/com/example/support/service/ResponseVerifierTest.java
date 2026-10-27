package com.example.support.service;

import com.example.support.model.TicketAnalysis;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseVerifierTest {

    private final ResponseVerifier verifier = new ResponseVerifier();

    @Test
    void invalidPriorityFallsBackAndHighRiskRequiresReview() {
        TicketAnalysis input = new TicketAnalysis(
                "PAYMENT", "urgent-ish", "negative", "Duplicate charge",
                "We will investigate.", "Escalate", false
        );

        TicketAnalysis output = verifier.verify(input, "I see a duplicate charge.");

        assertThat(output.priority()).isEqualTo("MEDIUM");
        assertThat(output.sentiment()).isEqualTo("NEGATIVE");
        assertThat(output.humanReviewRequired()).isTrue();
    }
}
