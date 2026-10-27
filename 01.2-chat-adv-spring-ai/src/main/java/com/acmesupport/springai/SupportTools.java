package com.acmesupport.springai;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
@Component
public class SupportTools {
    @Tool(description = "Return customer support opening hours.")
    public String supportHours() {
        return "Support is available Monday to Friday, 09:00 to 18:00 IST.";
    }
    @Tool(description = "Look up the demo status for a ticket by ticket ID.")
    public String ticketStatus(String ticketId) {
        return switch (ticketId == null ? "" : ticketId.toUpperCase()) {
            case "TCK-1002" -> "TCK-1002 status: IN_PROGRESS. Demo data only.";
            case "TCK-1003" -> "TCK-1003 status: WAITING_FOR_CUSTOMER. Demo data only.";
            default -> "No demo ticket found for " + ticketId + ".";
        };
    }
}
