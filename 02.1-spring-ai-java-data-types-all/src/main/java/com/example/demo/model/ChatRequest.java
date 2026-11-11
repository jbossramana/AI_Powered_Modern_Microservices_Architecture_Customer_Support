package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
