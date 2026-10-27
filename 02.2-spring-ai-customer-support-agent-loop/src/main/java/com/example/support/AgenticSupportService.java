
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

@Service
public class AgenticSupportService {

    // Logger to display the agent's execution steps in the console.
    private static final Logger log =
            LoggerFactory.getLogger(AgenticSupportService.class);

    // Prevent the agent from calling tools indefinitely.
    private static final int MAX_ITERATIONS = 5;

    // Sends prompts to the LLM and receives its responses.
    private final ChatModel chatModel;

    // Executes the tool calls requested by the LLM.
    private final ToolCallingManager toolCallingManager;

    // Contains all tools that the LLM is allowed to request.
    private final ToolCallback[] toolCallbacks;

    public AgenticSupportService(
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            SupportTools supportTools,
            AccountLookupTool accountLookupTool) {

        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;

        // Register the tools that the LLM can use.
        // For example: ticket lookup, account lookup, and plan lookup.
        this.toolCallbacks =
                ToolCallbacks.from(supportTools, accountLookupTool);
    }

    public String ask(String question) {

        // Configure the Ollama model and make the registered tools
        // available to the LLM.
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model("llama3.2:latest")
                .toolCallbacks(toolCallbacks)
                .temperature(0.1)
                .build();

        // SystemMessage defines the assistant's role and behavior.
        // UserMessage contains the actual customer question.
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("""
                            You are Aria, a customer-support assistant.

                            Use tools to retrieve account, ticket, and plan
                            facts when needed.

                            Never invent customer or ticket data.
                            If a tool says not found, say so.

                            After reviewing tool results, give a concise answer.
                            """),

                        new UserMessage(question)
                ),
                options
        );

        log.info("[AGENT] Starting manual execution loop. Question={}",
                question);

        // First LLM call:
        // The model may return a direct answer or request one or more tools.
        ChatResponse response = chatModel.call(prompt);

        // Java controls how many times the model can request and use tools.
        for (int iteration = 1;
             iteration <= MAX_ITERATIONS;
             iteration++) {

            log.info("[AGENT] Iteration {} of {}",
                    iteration, MAX_ITERATIONS);

            // If the LLM has not requested any tools, its response
            // is treated as the final answer.
            if (!response.hasToolCalls()) {

                String answer =
                        response.getResult().getOutput().getText();

                log.info("[AGENT] Model returned final answer; loop finished.");

                return answer;
            }

            // Display the names of the tools requested by the LLM.
            log.info("[AGENT] LLM requested tool call(s): {}",
                    response.getResult()
                            .getOutput()
                            .getToolCalls()
                            .stream()
                            .map(call -> call.name())
                            .toList());

            // Execute the LLM-requested tools.
            // Spring AI's ToolCallingManager invokes the matching Java tools
            // and adds their results to the conversation history.
            ToolExecutionResult result =
                    toolCallingManager.executeToolCalls(prompt, response);

            // Some tools can request that their result be returned directly
            // without another LLM call.
            if (result.returnDirect()) {

                log.info("[AGENT] A tool requested direct return; stopping model loop.");

                // Return the most recent conversation message.
                return result.conversationHistory()
                        .get(result.conversationHistory().size() - 1)
                        .getText();
            }

            // Preserve the complete conversation:
            // 1. Previous user message
            // 2. Assistant's tool-call request
            // 3. Tool execution results
            //
            // The LLM needs this history to decide what to do next.
            prompt = new Prompt(result.conversationHistory(), options);

            // Do not make another LLM call if the iteration limit is reached.
            if (iteration == MAX_ITERATIONS) {

                log.warn("[AGENT] Iteration limit reached; refusing to continue.");

                return "I could not complete the request within the allowed "
                        + "tool-call limit. Please try a more specific "
                        + "question or contact support.";
            }

            // Send the updated conversation back to the LLM.
            // After examining the tool results, the LLM may:
            // - Request another tool
            // - Request a different tool
            // - Return the final answer
            response = chatModel.call(prompt);
        }

        // Fallback response if the loop exits unexpectedly.
        return "The request could not be completed.";
    }
}
