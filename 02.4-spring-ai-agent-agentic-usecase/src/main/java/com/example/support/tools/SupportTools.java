package com.example.support.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Demo tool adapter. These methods return simulated backend data.
 * Replace each method body with an authenticated call to the real service/database.
 * All tools are read-only; no refund, password reset, escalation, or other action is performed.
 */
@Component
public class SupportTools {
    private static final Logger log = LoggerFactory.getLogger(SupportTools.class);

    @Tool(description = "Retrieve the latest invoice summary for a customer. Input customerId, for example C-1001.")
    public String getLatestInvoice(String customerId) {
        log.info("TOOL getLatestInvoice customerId={}", customerId);
        if ("C-1001".equalsIgnoreCase(customerId)) {
            return "Invoice INV-1001 for customer C-1001: total INR 1200.00, invoice date 2026-10-20, status PAID.";
        }
        return "No invoice found for the supplied customer ID in the demo dataset.";
    }

    @Tool(description = "Retrieve invoice details by invoice ID, including invoice total and line item summary.")
    public String getInvoiceDetails(String invoiceId) {
        log.info("TOOL getInvoiceDetails invoiceId={}", invoiceId);
        if ("INV-1001".equalsIgnoreCase(invoiceId)) {
            return "Invoice INV-1001: subscription INR 1000.00, tax INR 200.00, total INR 1200.00.";
        }
        return "No invoice found for the supplied invoice ID in the demo dataset.";
    }

    @Tool(description = "Get current support ticket status, priority, category and created date by ticket ID.")
    public String getTicketStatus(String ticketId) {
        log.info("TOOL getTicketStatus ticketId={}", ticketId);
        if ("TCK-1001".equalsIgnoreCase(ticketId)) {
            return "Ticket TCK-1001: status IN_PROGRESS, priority P1, category CLOUD_LOGIN, created 2026-10-21T09:00:00+05:30.";
        }
        return "No ticket found for the supplied ticket ID in the demo dataset.";
    }

    @Tool(description = "Get the applicable SLA target and current elapsed time for a ticket.")
    public String getTicketSla(String ticketId) {
        log.info("TOOL getTicketSla ticketId={}", ticketId);
        if ("TCK-1001".equalsIgnoreCase(ticketId)) {
            return "Demo SLA: P1 response target 15 minutes; resolution target 4 hours. Current elapsed time is 5 hours. This indicates the target time has passed; confirm operational timestamps in the real SLA service.";
        }
        return "No SLA data found for the supplied ticket ID in the demo dataset.";
    }

    @Tool(description = "Get ticket assignment and escalation history by ticket ID.")
    public String getTicketHistory(String ticketId) {
        log.info("TOOL getTicketHistory ticketId={}", ticketId);
        if ("TCK-1001".equalsIgnoreCase(ticketId)) {
            return "Demo history: assigned to Cloud Support at 09:05; last public update 10:30; no escalation event is present in this simulated history.";
        }
        return "No ticket history found for the supplied ticket ID in the demo dataset.";
    }

    @Tool(description = "Check the cloud platform's current demo service health.")
    public String getServiceHealth(String serviceName) {
        log.info("TOOL getServiceHealth serviceName={}", serviceName);
        return "Demo health for " + serviceName + ": DEGRADED; elevated upload latency observed. This is simulated data, not a live monitoring result.";
    }

    @Tool(description = "Get deployment details by deployment ID, including time and service.")
    public String getDeploymentHistory(String deploymentId) {
        log.info("TOOL getDeploymentHistory deploymentId={}", deploymentId);
        if ("DEP-2001".equalsIgnoreCase(deploymentId)) {
            return "Demo deployment DEP-2001: service cloud-upload-api, deployed 2026-10-24T18:00:00+05:30, version 2.4.1.";
        }
        return "No deployment found for the supplied deployment ID in the demo dataset.";
    }

    @Tool(description = "Get demo performance metrics for a service. Use a service name such as cloud-upload-api.")
    public String getPerformanceMetrics(String serviceName) {
        log.info("TOOL getPerformanceMetrics serviceName={}", serviceName);
        return "Demo metrics for " + serviceName + ": p95 latency increased from 300 ms to 2400 ms after the sample deployment; error rate increased from 0.5% to 6%. These are illustrative values, not live metrics.";
    }

    @Tool(description = "Get a customer's plan and storage allowance using customer ID.")
    public String getCustomerPlan(String customerId) {
        log.info("TOOL getCustomerPlan customerId={}", customerId);
        if ("C-1001".equalsIgnoreCase(customerId)) {
            return "Customer C-1001: Cloud Pro plan; storage allowance 500 GB.";
        }
        return "No plan found for the supplied customer ID in the demo dataset.";
    }

    @Tool(description = "Get a customer's simulated storage usage by customer ID.")
    public String getStorageUsage(String customerId) {
        log.info("TOOL getStorageUsage customerId={}", customerId);
        if ("C-1001".equalsIgnoreCase(customerId)) {
            return "Customer C-1001 current storage usage: 320 GB of 500 GB; usage is below the plan allowance.";
        }
        return "No storage usage data found for the supplied customer ID in the demo dataset.";
    }

    @Tool(description = "Get recent authentication diagnostic summary for a customer ID. Never return passwords or secrets.")
    public String getAuthenticationDiagnostics(String customerId) {
        log.info("TOOL getAuthenticationDiagnostics customerId={}", customerId);
        if ("C-1001".equalsIgnoreCase(customerId)) {
            return "Demo authentication diagnostics for C-1001: account ACTIVE; recent login failures show AUTH_SERVICE_TIMEOUT. No password or credential data is exposed.";
        }
        return "No authentication diagnostic data found for the supplied customer ID in the demo dataset.";
    }

    @Tool(description = "Get transaction summary for a customer ID. Use to compare charges with invoices; this does not initiate refunds.")
    public String getTransactionSummary(String customerId) {
        log.info("TOOL getTransactionSummary customerId={}", customerId);
        if ("C-1001".equalsIgnoreCase(customerId)) {
            return "Demo transactions for C-1001 related to INV-1001: transaction TX-9001 INR 1200.00 status CAPTURED; transaction TX-9002 INR 1200.00 status PENDING. A pending entry is not proof of a second captured charge. Verify with the payment provider before concluding.";
        }
        return "No transaction data found for the supplied customer ID in the demo dataset.";
    }
}
