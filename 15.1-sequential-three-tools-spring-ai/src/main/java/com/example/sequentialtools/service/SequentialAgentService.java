package com.example.sequentialtools.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.sequentialtools.model.InvestigationModels;

@Service
public class SequentialAgentService {
	private static final Set<String> ALLOWED = Set.of("CHECK_PAYMENT", "CHECK_ORDER", "CHECK_INVENTORY", "DONE");
	private final ChatClient chatClient;
	private final SequentialToolExecutor executor;

	public SequentialAgentService(ChatClient.Builder builder, SequentialToolExecutor executor) {
		this.chatClient = builder.build();
		this.executor = executor;
	}

	public InvestigationModels.InvestigationResponse investigate(String orderId, String question, int requestedMax) {
		int max = Math.max(1, Math.min(requestedMax, 3));
		List<InvestigationModels.ToolObservation> observations = new ArrayList<>();
		Set<String> executed = new LinkedHashSet<>();
		String finalMessage = null;
		boolean completed = false;
		System.out.println("\n================ SEQUENTIAL TOOL AGENT START ================");
		System.out.println("Order: " + orderId + " | Question: " + question + " | Max tools: " + max);
		for (int turn = 1; turn <= max + 1; turn++) {
			System.out.println("\n----- LLM TURN " + turn + " -----");
			System.out.println("[APP -> LLM] Send current observations; request exactly ONE next action.");
			InvestigationModels.NextToolDecision decision = requestDecision(orderId, question, observations, executed,
					max);
			if (decision == null || decision.toolName() == null) {
				finalMessage = "The model did not return a valid action. Please retry.";
				break;
			}
			String action = decision.toolName().trim().toUpperCase(Locale.ROOT);
			System.out.println("[LLM MESSAGE] " + safe(decision.message()));
			System.out.println("[LLM REASON ] " + safe(decision.reason()));
			System.out.println("[LLM REQUEST] " + action);
			if (!ALLOWED.contains(action)) {
				finalMessage = "Unsupported action requested; stopped safely.";
				break;
			}
			if ("DONE".equals(action)) {
				finalMessage = decision.message();
				completed = true;
				break;
			}
			if (executed.contains(action)) {
				finalMessage = "The model tried to repeat a tool; stopped safely.";
				break;
			}
			System.out.println("[SPRING APP] Dispatching exactly one tool.");
			System.out.println("[TOOL START] " + action + "(\"" + orderId + "\")");
			try {
				InvestigationModels.ToolObservation observation = executor.executeOne(action, orderId);
				observations.add(observation);
				executed.add(action);
				System.out.println("[TOOL RESULT] " + observation.result());
				System.out.println("[TOOL END] " + action);
				System.out.println("[SPRING APP -> LLM] Result appended. Request next message/action now.");
			} catch (Exception e) {
				finalMessage = "A tool failed; investigation stopped safely.";
				System.out.println("[TOOL ERROR] " + e.getMessage());
				break;
			}
			if (observations.size() >= max) {
				System.out.println("[LIMIT] Tool-call limit reached; ask LLM for a final summary.");
				finalMessage = finalSummary(orderId, question, observations);
				completed = true;
				break;
			}
		}
		if (finalMessage == null || finalMessage.isBlank())
			finalMessage = finalSummary(orderId, question, observations);
		System.out.println("\n----- FINAL LLM MESSAGE -----\n" + finalMessage);
		System.out.println("Total tools executed: " + observations.size());
		System.out.println("================ SEQUENTIAL TOOL AGENT END ==================\n");
		return new InvestigationModels.InvestigationResponse(orderId, finalMessage, List.copyOf(observations),
				observations.size(), completed);
	}

	private InvestigationModels.NextToolDecision requestDecision(String id, String question,
			List<InvestigationModels.ToolObservation> observations, Set<String> executed, int max) {
		String state = "Order ID: " + id + "\nCustomer question: " + question + "\nExecuted tools: " + executed
				+ "\nMax tool calls: " + max + "\nObservations: " + observations + "\nChoose exactly one next action.";
		try {
			return chatClient.prompt()
					.system("""
							You are a customer-support investigation agent. Return one structured decision only.
							toolName must be exactly CHECK_PAYMENT, CHECK_ORDER, CHECK_INVENTORY, or DONE.
							Select only ONE action per turn. Never select a tool already listed as executed.
							Preferred sequence: payment, order/shipment, inventory, then DONE, unless the evidence suggests otherwise.
							message: briefly explain what is known so far and what you plan to check next; for DONE, give the final answer.
							reason: one short reason for this action. Use only supplied facts. Never claim you changed/refunded/shipped anything.
							""")
					.user(state).call().entity(InvestigationModels.NextToolDecision.class);
		} catch (Exception e) {
			System.out.println("[LLM STRUCTURED-OUTPUT ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if (!executed.contains("CHECK_PAYMENT"))
				return new InvestigationModels.NextToolDecision("CHECK_PAYMENT", "I will verify payment first.",
						"Bounded Java fallback.");
			if (!executed.contains("CHECK_ORDER"))
				return new InvestigationModels.NextToolDecision("CHECK_ORDER",
						"Payment was checked. I will inspect order status next.", "Bounded Java fallback.");
			if (!executed.contains("CHECK_INVENTORY"))
				return new InvestigationModels.NextToolDecision("CHECK_INVENTORY",
						"Order status was checked. I will inspect inventory next.", "Bounded Java fallback.");
			return new InvestigationModels.NextToolDecision("DONE",
					"All three checks are complete. Review the returned findings.", "All tools checked.");
		}
	}

	private String finalSummary(String id, String question, List<InvestigationModels.ToolObservation> observations) {
		try {
			return chatClient.prompt().system(
					"Summarize the investigation using only the observations. Explain payment, order/shipment, and stock facts when available. Recommend a safe next step. Do not claim any action was executed.")
					.user("Order: " + id + "\nQuestion: " + question + "\nObservations: " + observations).call()
					.content();
		} catch (Exception e) {
			return "Investigation observations: " + observations + ". Review these findings before taking action.";
		}
	}

	private String safe(String s) {
		return s == null || s.isBlank() ? "(no message)" : s;
	}
}
