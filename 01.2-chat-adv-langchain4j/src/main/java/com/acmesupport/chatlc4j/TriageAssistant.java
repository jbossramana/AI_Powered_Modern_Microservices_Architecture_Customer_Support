package com.acmesupport.chatlc4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import java.util.List;

public interface TriageAssistant {
	@SystemMessage("Classify a customer support ticket. Return a typed result. Be conservative: urgency HIGH only for outages, security incidents, or major business impact. Never invent facts.")
	TicketTriage triage(@UserMessage String ticketText);

	record TicketTriage(String category, String priority, String sentiment, String summary,
			List<String> recommendedActions) {
	}
}
