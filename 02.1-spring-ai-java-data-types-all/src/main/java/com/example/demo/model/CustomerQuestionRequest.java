package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record CustomerQuestionRequest(
        @NotBlank String question,
        Long customerId
) {
}
