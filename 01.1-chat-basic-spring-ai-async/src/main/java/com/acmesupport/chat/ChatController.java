
package com.acmesupport.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * REST controller for the Acme customer support chat API.
 *
 * Handles HTTP requests and returns AI-generated responses
 * asynchronously using CompletableFuture.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final Executor chatExecutor;

    /**
     * Constructor injection.
     *
     * @param supportChatClient the configured Spring AI ChatClient
     * @param chatExecutor the executor for asynchronous chat processing
     */
    public ChatController(
            ChatClient supportChatClient,
            @Qualifier("chatExecutor") Executor chatExecutor) {

        this.chatClient = supportChatClient;
        this.chatExecutor = chatExecutor;
    }

    /**
     * Request DTO containing the customer's message.
     */
    public record ChatRequest(@NotBlank String message) {
    }

    /**
     * Response DTO containing the AI-generated reply.
     */
    public record ChatResponse(String reply) {
    }

    /**
     * Handles POST /api/chat asynchronously.
     *
     * Spring MVC sends the response when the CompletableFuture completes.
     *
     * @param request the customer's question
     * @return a future containing the AI-generated response
     */
    @PostMapping("/api/chat")
    public CompletableFuture<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {
    	
    	   System.out.println("Controller thread: "  + Thread.currentThread().getName());
    	

        return CompletableFuture.supplyAsync(() -> {
        	
            // Thread executing the LLM call.
            System.out.println("LLM thread: "
                    + Thread.currentThread().getName());

            // Build the prompt and provide the customer's message.
            String reply = chatClient.prompt()
                    .user(request.message())

                    // Invoke the configured language model.
                    .call()

                    // Extract the generated response text.
                    .content();

            // Return the answer as JSON when the future completes.
            return new ChatResponse(reply);

        }, chatExecutor);
    }
}
