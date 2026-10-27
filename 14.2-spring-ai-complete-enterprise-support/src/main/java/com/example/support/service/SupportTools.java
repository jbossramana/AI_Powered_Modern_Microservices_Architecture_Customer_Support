package com.example.support.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class SupportTools {

    @Tool(description = "Read-only lookup of a support ticket by ticket ID.")
    public String checkTicket(String ticketId) {
        return switch (ticketId) {
            case "TCK-100" -> "TCK-100: customer reports delayed shipment; priority medium; status open.";
            case "TCK-200" -> "TCK-200: duplicate payment concern; priority high; status open.";
            default -> "No ticket found for " + ticketId + ".";
        };
    }

    @Tool(description = "Read-only lookup of order status by order ID.")
    public String checkOrder(String orderId) {
        return switch (orderId) {
            case "ORD-101" -> "ORD-101: shipped; carrier scan says delayed.";
            case "ORD-202" -> "ORD-202: processing.";
            default -> "No order found for " + orderId + ".";
        };
    }

    @Tool(description = "Read-only lookup of payment status by order ID.")
    public String checkPayment(String orderId) {
        return switch (orderId) {
            case "ORD-101" -> "ORD-101 payment: captured.";
            case "ORD-202" -> "ORD-202 payment: authorized but not captured.";
            default -> "No payment record found for " + orderId + ".";
        };
    }

    @Tool(description = "Read-only inventory lookup by SKU.")
    public String checkInventory(String sku) {
        return sku.equalsIgnoreCase("SKU-101")
                ? "SKU-101 inventory: 8 units available."
                : "Inventory status unknown for " + sku + ".";
    }

    @Tool(description = "Read-only warranty policy lookup for a product.")
    public String checkWarranty(String productCode) {
        return "Warranty eligibility for " + productCode
                + " must be checked against purchase date and the official warranty policy.";
    }

    @Tool(description = "Read-only SLA lookup for a priority.")
    public String checkSla(String priority) {
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> "Critical: immediate escalation required.";
            case "HIGH" -> "High: prioritize for prompt human review.";
            default -> "Follow the standard support queue SLA for " + priority + ".";
        };
    }
}
