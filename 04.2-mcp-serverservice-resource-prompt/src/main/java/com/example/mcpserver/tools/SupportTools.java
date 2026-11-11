package com.example.mcpserver.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SupportTools {

    @McpTool(
            name = "checkDuplicatePayment",
            description = "Check whether a customer subscription has duplicate successful payments")
    public String checkDuplicatePayment(
            @McpToolParam(description = "Customer identifier", required = true)
            String customerId,
            @McpToolParam(description = "Subscription identifier", required = true)
            String subscriptionId) {

        // Demo data only. Replace this with a Billing Service API/database call.
        if ("C1001".equalsIgnoreCase(customerId)
                && "SUB1001".equalsIgnoreCase(subscriptionId)) {
            return """
                    Duplicate payment detected.
                    Customer: C1001
                    Subscription: SUB1001
                    Transaction TX101: INR 5000, SUCCESS
                    Transaction TX102: INR 5000, SUCCESS
                    Both transactions have the same amount.
                    """;
        }

        return """
                No duplicate payment found in the demo data.
                Customer: %s
                Subscription: %s
                """.formatted(customerId, subscriptionId);
    }

    @McpTool(
            name = "getTicket",
            description = "Retrieve a support ticket by its ticket identifier")
    public String getTicket(
            @McpToolParam(description = "Ticket identifier", required = true)
            String ticketId) {

        if ("T1001".equalsIgnoreCase(ticketId)) {
            return """
                    Ticket ID: T1001
                    Subject: Duplicate subscription charge
                    Status: OPEN
                    Priority: HIGH
                    Assignment: BILLING_L2
                    """;
        }

        return "No demo ticket found for ticket ID " + ticketId;
    }

    @McpTool(
            name = "createRefundRequest",
            description = "Create a demo refund request after duplicate payment verification")
    public String createRefundRequest(
            @McpToolParam(description = "Customer identifier", required = true)
            String customerId,
            @McpToolParam(description = "Subscription identifier", required = true)
            String subscriptionId,
            @McpToolParam(description = "Reason for the refund", required = true)
            String reason) {

        // Demonstration only: this returns a simulated request and does not move money.
        return """
                DEMO REFUND REQUEST CREATED
                Refund Request ID: REF-DEMO-1001
                Customer: %s
                Subscription: %s
                Reason: %s
                Status: PENDING_REVIEW
                Note: No actual payment or refund was processed.
                """.formatted(customerId, subscriptionId, reason);
    }
}
