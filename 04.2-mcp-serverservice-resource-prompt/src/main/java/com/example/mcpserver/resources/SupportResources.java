package com.example.mcpserver.resources;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
public class SupportResources {

    @McpResource(
            uri = "support://policies/refund",
            name = "Refund Policy",
            description = "Customer refund policy",
            mimeType = "text/plain")
    public String refundPolicy() {
        return """
                CUSTOMER REFUND POLICY

                1. A customer can request a refund within 7 days of payment.
                2. Refund requests are reviewed by the billing team.
                3. Duplicate payments must be verified against successful transactions.
                4. A refund must not be promised before billing verification.
                5. Approved refunds are normally processed within 5 business days.
                6. The demo application does not issue real refunds.
                """;
    }

    @McpResource(
            uri = "support://policies/payment",
            name = "Payment Policy",
            description = "Customer payment and duplicate-charge policy",
            mimeType = "text/plain")
    public String paymentPolicy() {
        return """
                CUSTOMER PAYMENT POLICY

                1. Verify payment using the customer and subscription identifiers.
                2. Compare successful transactions and transaction references.
                3. A customer statement alone is not proof of a duplicate charge.
                4. Escalate uncertain cases to the billing team.
                """;
    }
}
