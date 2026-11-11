package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank String ticketText,
        String documentation
) {
}
