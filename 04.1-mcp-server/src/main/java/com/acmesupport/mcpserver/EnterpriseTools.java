package com.acmesupport.mcpserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Demo enterprise data exposed as discoverable MCP tools. Replace in-memory maps with
 * secured CRM, ticketing and service-catalog adapters in production. */
@Service
public class EnterpriseTools {
    private static final Logger log = LoggerFactory.getLogger(EnterpriseTools.class);

    public record Customer(String id, String name, String email, String plan, String memberSince) {}
    public record Ticket(String id, String customerId, String subject, String status, String createdAt, String priority) {}
    public record SlaPolicy(String plan, String availability, String responseTarget, String breachAction) {}

    private final Map<String, Customer> customersById = Map.of(
            "C-1001", new Customer("C-1001", "Jordan Lee", "jordan.lee@example.com", "Pro", "2023-03-11"),
            "C-1002", new Customer("C-1002", "Amara Chen", "amara.chen@example.com", "Business", "2021-08-02"));
    private final Map<String, Ticket> ticketsById = Map.of(
            "TCK-1001", new Ticket("TCK-1001", "C-1001", "Can't access shared folder Q3 Reports", "OPEN", "2026-08-14", "MEDIUM"),
            "TCK-1002", new Ticket("TCK-1002", "C-1002", "Billing charge of $29.99 looks wrong", "IN_PROGRESS", "2026-08-13", "HIGH"),
            "TCK-1003", new Ticket("TCK-1003", "C-1002", "Team unable to access accounts after outage", "OPEN", "2026-08-15", "CRITICAL"));
    private final Map<String, SlaPolicy> slaByPlan = Map.of(
            "FREE", new SlaPolicy("Free", "No uptime commitment", "Best effort", "No service credits"),
            "PRO", new SlaPolicy("Pro", "99.5% monthly uptime", "Next business day", "Review contract terms"),
            "BUSINESS", new SlaPolicy("Business", "99.9% monthly uptime", "4 hours", "Service credits may apply"));

    @Tool(description = "Look up a customer's account record by exact customer ID in the enterprise CRM. Returns a not-found message for unknown IDs.")
    public Object getCustomer(@ToolParam(description = "Customer ID, for example C-1002") String customerId) {
        String id = normalize(customerId);
        log.info("MCP tool invoked: getCustomer customerId={}", id);
        return customersById.getOrDefault(id, null) != null ? customersById.get(id) : "No customer found with ID " + id;
    }

    @Tool(description = "Get all ticket history for a customer, including open, in-progress and closed tickets. Use a customer ID such as C-1002.")
    public List<Ticket> getTicketHistory(@ToolParam(description = "Customer ID, for example C-1002") String customerId) {
        String id = normalize(customerId);
        log.info("MCP tool invoked: getTicketHistory customerId={}", id);
        if (!customersById.containsKey(id)) return List.of();
        return ticketsById.values().stream().filter(t -> t.customerId().equals(id)).toList();
    }

    @Tool(description = "Find one ticket by exact ticket ID, including status and priority.")
    public Object getTicketById(@ToolParam(description = "Ticket ID, for example TCK-1003") String ticketId) {
        String id = normalize(ticketId);
        log.info("MCP tool invoked: getTicketById ticketId={}", id);
        return ticketsById.getOrDefault(id, null) != null ? ticketsById.get(id) : "No ticket found with ID " + id;
    }

    @Tool(description = "Get SLA policy for a subscription plan, including uptime, response target and breach action. Valid plans: Free, Pro, Business.")
    public Object getPlanSla(@ToolParam(description = "Plan name: Free, Pro, or Business") String planName) {
        String plan = normalize(planName);
        log.info("MCP tool invoked: getPlanSla plan={}", plan);
        return slaByPlan.getOrDefault(plan, null) != null ? slaByPlan.get(plan) : "Unknown plan. Valid plans are Free, Pro, and Business.";
    }

    @Tool(description = "Return a compact customer support snapshot: customer record, tickets, and applicable SLA. Use this when the user asks for an overall customer summary.")
    public Object getCustomerSupportSnapshot(@ToolParam(description = "Customer ID, for example C-1002") String customerId) {
        String id = normalize(customerId);
        log.info("MCP tool invoked: getCustomerSupportSnapshot customerId={}", id);
        Customer customer = customersById.get(id);
        if (customer == null) return "No customer found with ID " + id;
        return Map.of("customer", customer,
                "tickets", ticketsById.values().stream().filter(t -> t.customerId().equals(id)).toList(),
                "sla", slaByPlan.get(customer.plan().toUpperCase()));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().toUpperCase();
    }
}
