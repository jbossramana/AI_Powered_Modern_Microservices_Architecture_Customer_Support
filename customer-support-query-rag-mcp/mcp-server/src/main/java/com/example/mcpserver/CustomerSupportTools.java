package com.example.mcpserver;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class CustomerSupportTools {

    @McpTool(
            name = "getOrderStatus",
            description = "Look up the status and payment summary for a customer order by its order ID.")
    public String getOrderStatus(
            @McpToolParam(description = "Order identifier, for example ORD-1001", required = true)
            String orderId) {

        if (orderId == null || orderId.isBlank()) {
            return "Order ID is required.";
        }

        return switch (orderId.trim().toUpperCase()) {
            case "ORD-1001" ->
                    "Order ORD-1001: status=DELIVERED; payment=PAID; "
                    + "payment note=the demo data does not confirm a duplicate charge; "
                    + "next step=verify payment records with the billing team.";
            case "ORD-1002" ->
                    "Order ORD-1002: status=PROCESSING; payment=PAID; "
                    + "estimated dispatch=within 2 business days (demo data).";
            default ->
                    "No demo order was found for " + orderId
                    + ". Ask the customer to verify the order ID or contact support.";
        };
    }

    @McpTool(
            name = "getTicketDetails",
            description = "Look up a customer support ticket by ticket ID and return its status and summary.")
    public String getTicketDetails(
            @McpToolParam(description = "Support ticket identifier, for example TCK-2001", required = true)
            String ticketId) {

        if (ticketId == null || ticketId.isBlank()) {
            return "Ticket ID is required.";
        }

        return switch (ticketId.trim().toUpperCase()) {
            case "TCK-2001" ->
                    "Ticket TCK-2001: status=IN_PROGRESS; category=duplicate payment; "
                    + "summary=billing team is reviewing the transaction; "
                    + "do not promise a refund date because no confirmed date is available.";
            case "TCK-2002" ->
                    "Ticket TCK-2002: status=RESOLVED; category=delivery; "
                    + "summary=delivery confirmation was sent to the customer.";
            default ->
                    "No demo ticket was found for " + ticketId
                    + ". Ask the customer to verify the ticket ID or contact support.";
        };
    }
}
