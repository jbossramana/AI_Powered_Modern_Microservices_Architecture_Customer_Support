package com.example.support;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Teaching example of a goal-driven, bounded agentic workflow.
 *
 * The LLM chooses the next tool/action and can revise its next step based on
 * tool results. Java still enforces iteration limits and safety boundaries.
 * This is not an unrestricted or background-running autonomous process.
 */
@Service
public class AgenticSupportService {
    private static final Logger log = LoggerFactory.getLogger(AgenticSupportService.class);
    private static final int MAX_STEPS = 8;

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] toolCallbacks;

    public AgenticSupportService(
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            SupportTools supportTools,
            AccountLookupTool accountLookupTool) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = ToolCallbacks.from(supportTools, accountLookupTool);
    }

    public String investigate(String customerGoal) {
   
    	OllamaChatOptions options = OllamaChatOptions.builder()
    	        .model("llama3.2:latest")
    	        .toolCallbacks(toolCallbacks)
    	        .temperature(0.1)
    	        .build();
        Prompt prompt = new Prompt(List.of(
        		new SystemMessage("""
        			    You are Aria, an agentic customer-support assistant.

        			    Your goal is to investigate the customer's question using tools
        			    before providing the final answer.

        				Work autonomously within this single request, Investigation rules:
        			    1. Identify every piece of information needed to answer the question.
        			    2. For a ticket investigation, call the ticket lookup tool.
        			    3. If the question asks about a support plan, SLA, or response target,
        			       call the account/plan lookup tool as well.
        			    4. Do not provide the final answer until all required information
        			       has been retrieved, unless a required tool fails.
        			    5. After each tool result, reassess what information is still missing.
        			    6. Request additional tools when necessary.
        			    7. Use only actual tool results. Never invent ticket, account,
        			       plan, or SLA details.
        			    8. When the investigation is complete, provide a concise answer
        			       supported by the retrieved data.
        			    """),
                new UserMessage("""
                    CUSTOMER GOAL:
                    %s

                    Decide the next useful action. Use tools when facts are needed.
                    After the relevant facts have been gathered, provide a final summary.
                    """.formatted(customerGoal))), options);

        log.info("[AGENTIC] Starting goal-directed investigation. Goal={}", customerGoal);
        ChatResponse response = chatModel.call(prompt);

        for (int step = 1; step <= MAX_STEPS; step++) {
            log.info("[AGENTIC] Decision/action cycle {}/{}", step, MAX_STEPS);

            if (!response.hasToolCalls()) {
                String answer = response.getResult().getOutput().getText();
                log.info("[AGENTIC] LLM decided no further tool action is required.");
                log.info("[AGENTIC] Goal investigation finished.");
                return answer;
            }

            var calls = response.getResult().getOutput().getToolCalls();
            log.info("[AGENTIC] LLM selected next action(s): {}",
                    calls.stream().map(call -> call.name()).toList());

            ToolExecutionResult execution = toolCallingManager.executeToolCalls(prompt, response);

            if (execution.returnDirect()) {
                log.info("[AGENTIC] Tool requested direct return; stopping.");
                var history = execution.conversationHistory();
                return history.get(history.size() - 1).getText();
            }

            prompt = new Prompt(execution.conversationHistory(), options);

            if (step == MAX_STEPS) {
                log.warn("[AGENTIC] Maximum cycle count reached; ending bounded investigation.");
                return "I gathered some information but reached the investigation step limit. Please review the available ticket/account details or narrow the request.";
            }

            // The LLM sees the previous tool result and autonomously chooses what to do next.
            response = chatModel.call(prompt);
        }

        return "The investigation could not be completed.";
    }
}
