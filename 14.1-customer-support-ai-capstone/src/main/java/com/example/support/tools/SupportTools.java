package com.example.support.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class SupportTools {

    @Tool(description = "Look up the current status of an order by order ID. Read-only.")
    public String checkOrder(String orderId) {
        return switch (orderId == null ? "" : orderId.trim().toUpperCase()) {
            case "ORD-10025" -> "Order ORD-10025: laptop; status=SHIPPMENT_DELAYED; expected delivery date has passed.";
            case "ORD-10026" -> "Order ORD-10026: headphones; status=SHIPPED; carrier scan updated today.";
            default -> "Order not found. Ask the customer to verify the order ID.";
        };
    }

    @Tool(description = "Check payment status by order ID. Read-only.")
    public String checkPayment(String orderId) {
        return switch (orderId == null ? "" : orderId.trim().toUpperCase()) {
            case "ORD-10025" -> "Payment for ORD-10025: SUCCESSFUL.";
            case "ORD-10026" -> "Payment for ORD-10026: SUCCESSFUL.";
            default -> "Payment record not found.";
        };
    }

    @Tool(description = "Check inventory availability for a product. Read-only.")
    public String checkInventory(String productName) {
        if (productName == null || productName.isBlank()) {
            return "Product name is required.";
        }
        String normalized = productName.toLowerCase();
        if (normalized.contains("laptop")) {
            return "Laptop inventory: 4 units available in the central warehouse; this does not confirm shipment status.";
        }
        if (normalized.contains("headphone")) {
            return "Headphones inventory: 18 units available.";
        }
        return "No inventory match found for the supplied product.";
    }
}
