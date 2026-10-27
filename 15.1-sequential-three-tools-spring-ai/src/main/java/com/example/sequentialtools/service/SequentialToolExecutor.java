package com.example.sequentialtools.service;

import com.example.sequentialtools.model.InvestigationModels;
import com.example.sequentialtools.tools.CustomerSupportTools;
import org.springframework.stereotype.Service;

@Service
public class SequentialToolExecutor {
	private final CustomerSupportTools tools;

	public SequentialToolExecutor(CustomerSupportTools tools) {
		this.tools = tools;
	}

	public InvestigationModels.ToolObservation executeOne(String toolName, String orderId) {
		return switch (toolName) {
		case "CHECK_PAYMENT" ->
			new InvestigationModels.ToolObservation(toolName, tools.checkPayment(orderId).toString());
		case "CHECK_ORDER" -> new InvestigationModels.ToolObservation(toolName, tools.checkOrder(orderId).toString());
		case "CHECK_INVENTORY" ->
			new InvestigationModels.ToolObservation(toolName, tools.checkInventory(orderId).toString());
		default -> throw new IllegalArgumentException("Tool is not allowed: " + toolName);
		};
	}
}
