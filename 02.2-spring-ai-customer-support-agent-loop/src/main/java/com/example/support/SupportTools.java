package com.example.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SupportTools {
    private static final Logger log = LoggerFactory.getLogger(SupportTools.class);

    // Demo data only. Replace these methods with secured service/repository calls in production.
    @Tool(description = "Look up the status and latest update for a support ticket using its ticket ID.")
    public String lookupTicket(
            @ToolParam(description = "Ticket ID, for example TICK-501") String ticketId) {
        log.info("[TOOL EXECUTION] lookupTicket arguments={{\"ticketId\":\"{}\"}}", ticketId);
        String result = switch (ticketId.toUpperCase()) {
            case "TICK-501" -> "{\"ticketId\":\"TICK-501\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"lastUpdate\":\"Engineer investigating login failures\"}";
            case "TICK-502" -> "{\"ticketId\":\"TICK-502\",\"status\":\"RESOLVED\",\"priority\":\"NORMAL\",\"lastUpdate\":\"Fix deployed; please retry\"}";
            default -> "{\"found\":false,\"ticketId\":\"" + ticketId + "\"}";
        };
        log.info("[TOOL RESULT] lookupTicket result={}", result);
        return result;
    }

    @Tool(description = "Retrieve the support plan features and service response target for a plan name.")
    public String lookupPlan(
            @ToolParam(description = "Plan name, for example Premium or Basic") String planName) {
        log.info("[TOOL EXECUTION] lookupPlan arguments={{\"planName\":\"{}\"}}", planName);
        String result = switch (planName.toLowerCase()) {
            case "premium" -> "{\"plan\":\"Premium\",\"features\":[\"24x7 support\",\"priority handling\"],\"initialResponseTarget\":\"1 hour\"}";
            case "basic" -> "{\"plan\":\"Basic\",\"features\":[\"Business-hours support\"],\"initialResponseTarget\":\"1 business day\"}";
            default -> "{\"found\":false,\"plan\":\"" + planName + "\"}";
        };
        log.info("[TOOL RESULT] lookupPlan result={}", result);
        return result;
    }
}
