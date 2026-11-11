package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record HistoryRequest(
        @NotBlank String previousUserMessage,
        @NotBlank String previousAssistantMessage,
        @NotBlank String currentQuestion
) {
}
