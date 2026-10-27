
package com.acmesupport.chatlc4j;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes an HTTP API for the customer support assistant.
 *
 * Responsibilities:
 * 1. Accept customer questions through a REST endpoint.
 * 2. Pass the question to SupportAssistant.
 * 3. Return the AI-generated answer as a JSON response.
 *
 * The controller handles HTTP communication; SupportAssistant handles
 * interaction with the AI model through LangChain4j.
 */
@RestController
public class ChatController {

    // Spring injects the SupportAssistant bean configured in AssistantConfig.
    private final SupportAssistant assistant;

    /**
     * Constructor injection.
     * Spring provides the SupportAssistant implementation created by AiServices.
     */
    public ChatController(SupportAssistant assistant) {
        this.assistant = assistant;
    }

    /**
     * Represents the JSON request received from the client.
     *
     * Example:
     * {
     *   "message": "How do I reset my password?"
     * }
     *
     * @NotBlank declares that the message must not be null, empty,
     * or contain only whitespace.
     */
    public record ChatRequest(@NotBlank String message) {
    }

    /**
     * Represents the JSON response returned to the client.
     *
     * Example:
     * {
     *   "reply": "To reset your password, use the Forgot Password option."
     * }
     */
    public record ChatResponse(String reply) {
    }

    /**
     * Handles POST requests to /api/chat.
     *
     * Processing flow:
     * 1. Spring converts the incoming JSON into ChatRequest.
     * 2. The controller sends the message to SupportAssistant.
     * 3. LangChain4j connects the assistant method to the configured ChatModel.
     * 4. The model generates a response.
     * 5. The controller wraps the response in ChatResponse.
     *
     * @param request contains the customer's question
     * @return the AI-generated reply as JSON
     */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        // Invoke the assistant interface; LangChain4j handles the model call.
        String reply = assistant.chat(request.message());

        // Return the generated answer in a structured JSON response.
        return new ChatResponse(reply);
    }
}
