
package com.example.support.service;

import java.util.List;

import com.example.support.tools.SupportTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
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

@Service
public class SupportAssistantService {

    private static final Logger log =
            LoggerFactory.getLogger(SupportAssistantService.class);

    private static final int MAX_TOOL_ROUNDS = 5;

    private static final String SYSTEM_RULES = """
            You are Aria, an enterprise customer-support assistant.
            Use tool results as the source of truth for backend facts.
            The tools in this demo return simulated data, not live data.
            Never invent ticket status, invoice totals, SLA values,
            or root causes.
            Never claim that a state-changing action was completed.
            Explain uncertainty when evidence is incomplete.
            Be concise, clear, and professional.
            """;

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallback[] toolCallbacks;
    private final String ollamaModel;

    public SupportAssistantService(
            ChatClient.Builder builder,
            ChatModel chatModel,
            SupportTools supportTools,
            ToolCallingManager toolCallingManager,
            @Value("${spring.ai.ollama.chat.model}") String ollamaModel) {

        this.chatClient = builder.build();
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = ToolCallbacks.from(supportTools);
        this.ollamaModel = ollamaModel;
    }

    /**
     * Framework-managed tool calling.
     * Spring AI manages the tool execution loop.
     */
    public String answerWithAgent(String message) {
        return chatClient.prompt()
                .system(SYSTEM_RULES)
                .user(message)
                .tools(toolCallbacks)
                .call()
                .content();
    }

    /**
     * Explicit agentic investigation.
     * Java controls the loop; the LLM selects tools based on
     * the goal and previous tool results.
     */
    public String investigateWithExplicitLoop(
            String identifiers,
            String goal) {

        String userMessage = """
                Investigate the support issue using the supplied identifiers.

                Decide which checks are relevant to the goal.
                Use tool results as evidence when deciding what to do next.
                Do not present an unverified cause as confirmed.
                Stop when sufficient evidence is available.
                Never claim that a state-changing action was completed.

                Identifiers:
                %s

                Investigation goal:
                %s
                """.formatted(identifiers, goal);

        // IMPORTANT: include the Ollama model in the prompt options.
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(ollamaModel)
                .toolCallbacks(toolCallbacks)
                .build();

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(SYSTEM_RULES),
                        new UserMessage(userMessage)
                ),
                options
        );

        log.info("Starting investigation with Ollama model: {}", ollamaModel);

        ChatResponse response = chatModel.call(prompt);
        int round = 0;

        while (response != null
                && response.hasToolCalls()
                && round < MAX_TOOL_ROUNDS) {

            round++;

            log.info(
                    "Agent investigation round {} of {}",
                    round,
                    MAX_TOOL_ROUNDS
            );

            // Execute the tools requested by the LLM.
            ToolExecutionResult result =
                    toolCallingManager.executeToolCalls(prompt, response);

            if (result.returnDirect()) {
                var history = result.conversationHistory();

                if (history.isEmpty()) {
                    return "The tool returned no response.";
                }

                return history.get(history.size() - 1).getText();
            }

            // Preserve tool calls and their results for the next LLM request.
            prompt = new Prompt(
                    result.conversationHistory(),
                    options
            );

            // Ask the model to decide what to do next.
            response = chatModel.call(prompt);
        }

        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            return "The investigation did not produce a final answer.";
        }

        if (response.hasToolCalls()) {
            return "The investigation reached the maximum of "
                    + MAX_TOOL_ROUNDS
                    + " tool rounds before a final answer was produced. "
                    + "Please narrow the investigation goal and try again.";
        }

        return response.getResult().getOutput().getText();
    }
}
