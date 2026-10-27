package com.example.support.service;

import com.example.support.model.InvestigationResult;
import org.springframework.stereotype.Service;

@Service
public class SupportWorkflowService {

    private final SupportTools tools;

    public SupportWorkflowService(SupportTools tools) {
        this.tools = tools;
    }

    /**
     * Deterministic workflow: Java controls the sequence. The model is not allowed
     * to decide which system operations execute.
     */
    public InvestigationResult investigate(String ticketId, String orderId, String sku) {
        String ticket = tools.checkTicket(ticketId);
        String order = tools.checkOrder(orderId);
        String payment = tools.checkPayment(orderId);
        String inventory = tools.checkInventory(sku);

        boolean humanApprovalRequired = payment.toLowerCase().contains("captured")
                || ticket.toLowerCase().contains("duplicate payment");

        String recommendation;
        if (payment.toLowerCase().contains("duplicate")) {
            recommendation = "Escalate payment concern for human investigation.";
        } else if (order.toLowerCase().contains("delayed")) {
            recommendation = "Explain the verified delay and check the latest carrier event.";
        } else {
            recommendation = "Review ticket details and follow the applicable support policy.";
        }

        return new InvestigationResult(
                ticketId,
                orderId,
                payment,
                order,
                inventory,
                recommendation,
                humanApprovalRequired
        );
    }
}
