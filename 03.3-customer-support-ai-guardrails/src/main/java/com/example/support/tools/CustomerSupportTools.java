package com.example.support.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CustomerSupportTools {
    @Tool(description = "Look up a demo support ticket by its ticket ID. Use when a customer asks for ticket status.")
    public String getTicketStatus(@ToolParam(description = "Ticket ID, for example TCK-1001") String ticketId) {
        if (ticketId == null || !ticketId.matches("TCK-[0-9]{4,8}")) return "Invalid ticket ID format. Expected TCK- followed by digits.";
        if (ticketId.equalsIgnoreCase("TCK-1001")) return "Ticket TCK-1001: status OPEN; assigned team Application Support; priority HIGH; last updated 2026-10-23. Demo data only.";
        if (ticketId.equalsIgnoreCase("TCK-1002")) return "Ticket TCK-1002: status IN_PROGRESS; assigned team Billing Support; priority NORMAL; last updated 2026-10-22. Demo data only.";
        return "No demo ticket found for " + ticketId + ". Ask the customer to verify the ticket ID or contact support.";
    }

    @Tool(description = "Check whether a demo refund request can be reviewed. This tool never issues a refund.")
    public String checkRefundEligibility(@ToolParam(description = "Order ID, for example ORD-2001") String orderId) {
        if (orderId == null || !orderId.matches("ORD-[0-9]{4,8}")) return "Invalid order ID format. Expected ORD- followed by digits.";
        if (orderId.equalsIgnoreCase("ORD-2001")) return "Order ORD-2001: duplicate payment flag found in demo data. Eligible for billing review; this is not approval and no refund has been issued.";
        return "No duplicate-payment flag found in demo data for " + orderId + ". Billing support must review the transaction before any refund decision.";
    }
}
