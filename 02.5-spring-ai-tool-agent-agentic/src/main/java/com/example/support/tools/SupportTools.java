package com.example.support.tools;

import com.example.support.domain.SupportDataService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tools exposed to the model. They only return demo data. Never expose unrestricted
 * destructive operations as model tools in a production application.
 */
@Component
public class SupportTools {
    private final SupportDataService data;

    public SupportTools(SupportDataService data) {
        this.data = data;
    }

    @Tool(description = "Look up the current status of a customer support ticket by ticket ID.")
    public String getTicketStatus(String ticketId) {
        return data.getTicketStatus(ticketId);
    }

    @Tool(description = "Look up the support plan for a customer using the customer ID.")
    public String getCustomerPlan(String customerId) {
        return data.getCustomerPlan(customerId);
    }

    @Tool(description = "Retrieve the support policy for a customer's plan and issue category.")
    public String getSupportPolicy(String plan, String category) {
        return data.getSupportPolicy(plan, category);
    }

    @Tool(description = "Retrieve order details for a customer and support ticket. Use as evidence, not as proof of refund eligibility.")
    public String getOrderDetails(String customerId, String ticketId) {
        return data.getOrderDetails(customerId, ticketId);
    }

    @Tool(description = "Retrieve payment investigation details for a customer and support ticket. Does not issue a refund.")
    public String getPaymentDetails(String customerId, String ticketId) {
        return data.getPaymentDetails(customerId, ticketId);
    }
}
