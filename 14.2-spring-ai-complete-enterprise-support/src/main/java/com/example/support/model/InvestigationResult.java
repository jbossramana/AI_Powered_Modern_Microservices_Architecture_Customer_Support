package com.example.support.model;

public record InvestigationResult(
        String ticketId,
        String orderId,
        String paymentStatus,
        String orderStatus,
        String inventoryStatus,
        String recommendation,
        boolean humanApprovalRequired
) {
}
