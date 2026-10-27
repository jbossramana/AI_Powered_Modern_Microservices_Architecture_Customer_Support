package com.example.support.model;

import jakarta.validation.constraints.NotBlank;

public record TicketRequest(
        @NotBlank String ticketId,
        @NotBlank String customerMessage
) {}
