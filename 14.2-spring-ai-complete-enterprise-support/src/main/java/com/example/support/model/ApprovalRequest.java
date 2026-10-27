package com.example.support.model;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequest(
        @NotBlank String ticketId,
        @NotBlank String action,
        @NotBlank String reason
) {
}
