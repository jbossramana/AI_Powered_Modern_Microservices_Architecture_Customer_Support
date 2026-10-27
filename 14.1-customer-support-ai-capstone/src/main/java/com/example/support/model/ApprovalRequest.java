package com.example.support.model;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequest(
        @NotBlank String ticketId,
        @NotBlank String approvedBy,
        @NotBlank String responseText,
        boolean approved
) {}
