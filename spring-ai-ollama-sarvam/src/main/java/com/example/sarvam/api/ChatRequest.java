package com.example.sarvam.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String question) {
}
