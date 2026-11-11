package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public record TemplateRequest(
        @NotBlank String topic,
        String audience
) {
}
