package com.example.support.model;

import jakarta.validation.constraints.NotBlank;

public record ApprovalDecision(
        @NotBlank String decision,
        String note
) {
}
