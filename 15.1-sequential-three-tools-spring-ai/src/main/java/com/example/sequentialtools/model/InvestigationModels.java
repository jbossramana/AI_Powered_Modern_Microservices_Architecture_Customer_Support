package com.example.sequentialtools.model;

import java.util.List;

public final class InvestigationModels {
	private InvestigationModels() {
	}

	public record InvestigateRequest(String orderId, String question, Integer maxToolCalls) {
	}

	public record NextToolDecision(String toolName, String message, String reason) {
	}

	public record ToolObservation(String toolName, String result) {
	}

	public record InvestigationResponse(String orderId, String finalMessage, List<ToolObservation> toolCalls,
			int totalToolCalls, boolean completed) {
	}
}
