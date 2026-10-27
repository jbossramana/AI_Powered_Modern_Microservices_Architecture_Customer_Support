
package com.acmesupport.chat;

import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the customer support chat API using Spring AI.
 *
 * Responsibilities:
 * 1. Accept a customer question through an HTTP POST request.
 * 2. Use Spring AI ChatClient to communicate with the configured LLM.
 * 3. Return the generated answer as JSON.
 *
 * ChatController handles HTTP requests, while ChatClient provides
 * a fluent API for interacting with the language model.
 */
@RestController
public class ChatController {

    // Spring injects the ChatClient bean configured elsewhere in the application.
    private final ChatClient chatClient;

    /**
     * Constructor injection.
     *
     * @param supportChatClient the configured Spring AI ChatClient
     */
    public ChatController(ChatClient supportChatClient) {
        this.chatClient = supportChatClient;
    }

    /**
     * Request DTO containing the customer's message.
     *
     * Example JSON:
     * {
     *   "message": "How do I reset my password?"
     * }
     *
     * @NotBlank declares that the message must not be empty or whitespace.
     */
    public record ChatRequest(@NotBlank String message) {
    }

    /**
     * Response DTO containing the AI-generated reply.
     *
     * Example JSON:
     * {
     *   "reply": "Select Forgot Password on the login page."
     * }
     */
    public record ChatResponse(String reply) {
    }

    /**
     * Handles POST /api/chat.
     *
     * Processing:
     * 1. Spring converts the JSON body into ChatRequest.
     * 2. ChatClient builds a prompt containing the user's message.
     * 3. call() invokes the configured language model.
     * 4. content() extracts the generated text.
     * 5. The answer is returned as a ChatResponse.
     *
     * @param request the customer's question
     * @return the AI-generated reply
     */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        // Build the prompt and provide the customer's message.
        String reply = chatClient.prompt()
                .user(request.message())

                // Send the request to the configured model.
                .call()

                // Extract the generated response text.
                .content();

        // Return the answer as JSON.
        return new ChatResponse(reply);
    }
}
