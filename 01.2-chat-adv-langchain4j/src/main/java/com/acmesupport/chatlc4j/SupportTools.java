package com.acmesupport.chatlc4j;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class SupportTools {
	@Tool("Look up the published support hours for Acme Cloud Storage")
	public String getSupportHours() {
		return "Support hours: Monday-Friday, 09:00-18:00 IST. For critical production outages, use the customer portal emergency channel.";
	}

	@Tool("Look up a sample ticket status by ticket ID. This is a local demo dataset, not a production system.")
	public String lookupTicketStatus(String ticketId) {
		if (ticketId == null || !ticketId.matches("TCK-[0-9]{4}"))
			return "Invalid demo ticket ID. Expected TCK-1234.";
		return switch (ticketId) {
		case "TCK-1001" -> "TCK-1001: OPEN; assigned to Support Tier 1; last updated today.";
		case "TCK-1002" -> "TCK-1002: IN_PROGRESS; assigned to Cloud Operations; next update pending.";
		default -> "No demo ticket found for " + ticketId + ". No real ticket system was queried.";
		};
	}
}
