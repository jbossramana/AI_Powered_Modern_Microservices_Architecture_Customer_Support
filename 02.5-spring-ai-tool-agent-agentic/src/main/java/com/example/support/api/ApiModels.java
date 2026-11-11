package com.example.support.api;

import jakarta.validation.constraints.NotBlank;

public final class ApiModels {
    private ApiModels() {}

    public record SingleToolRequest(@NotBlank String customerId) {}
    public record FixedWorkflowRequest(@NotBlank String ticketId, @NotBlank String customerId) {}
    public record QuestionRequest(@NotBlank String question) {}
    public record AgenticRequest(@NotBlank String goal, String ticketId, String customerId) {}
    public record MultiAgentRequest(@NotBlank String message, @NotBlank String ticketId, @NotBlank String customerId) {}

    public record DemoResponse(String pattern, Object result, Object details) {}
}
