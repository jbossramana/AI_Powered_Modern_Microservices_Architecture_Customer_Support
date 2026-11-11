package com.example.support.domain;

import org.springframework.stereotype.Service;

/**
 * Deterministic in-memory data for the demo. Replace this class with repository/API calls
 * in a real application. The service is the source of truth; the LLM is not.
 */
@Service
public class SupportDataService {

    public String getCustomerPlan(String customerId) {
        return switch (customerId) {
            case "C-1001" -> "Pro";
            case "C-1002" -> "Business";
            default -> "Standard";
        };
    }

    public String getTicketStatus(String ticketId) {
        return switch (ticketId) {
            case "TCK-1001" -> "OPEN";
            case "TCK-1002" -> "IN_PROGRESS";
            case "TCK-1003" -> "RESOLVED";
            default -> "NOT_FOUND";
        };
    }

    public String getTicketCategory(String ticketId) {
        return switch (ticketId) {
            case "TCK-1001" -> "BILLING";
            case "TCK-1002" -> "LOGIN";
            case "TCK-1003" -> "DELIVERY";
            default -> "UNKNOWN";
        };
    }

    public String getSupportPolicy(String plan, String category) {
        if ("Business".equalsIgnoreCase(plan)) {
            return "Business plan: priority support. Verify the ticket and applicable policy before promising a resolution.";
        }
        if ("BILLING".equalsIgnoreCase(category)) {
            return "Billing policy: verify payment/order records before confirming a duplicate charge or refund eligibility.";
        }
        return "Standard support policy applies. Verify facts before promising a resolution.";
    }

    public String getPaymentDetails(String customerId, String ticketId) {
        if ("C-1001".equals(customerId) && "TCK-1001".equals(ticketId)) {
            return "DEMO DATA: two matching payment entries are flagged for verification; this is not proof that a refund is due.";
        }
        return "No matching duplicate-payment flag in demo data. Check authoritative payment records.";
    }

    public String getOrderDetails(String customerId, String ticketId) {
        return "Demo order lookup for customer " + customerId + " and ticket " + ticketId
                + ": order exists; payment reconciliation is still required.";
    }

    public String createRefundRequest(String customerId, String ticketId, String reason) {
        // Demo safety: this method does NOT issue a refund or persist a real request.
        return "DEMO ONLY: refund request was not submitted. A real implementation must re-check eligibility, authorize the caller, and require approval.";
    }
}
