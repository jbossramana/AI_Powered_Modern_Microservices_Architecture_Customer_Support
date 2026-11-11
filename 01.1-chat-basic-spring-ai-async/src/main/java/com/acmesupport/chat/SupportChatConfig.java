
package com.acmesupport.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient for the Acme customer support application.
 *
 * Responsibilities:
 * 1. Define Aria's role and behavior through a system prompt.
 * 2. Receive the ChatModel configured by Spring Boot.
 * 3. Build a reusable ChatClient bean.
 *
 * ChatModel is the lower-level abstraction that communicates with the
 * configured LLM provider, such as Ollama.
 *
 * ChatClient is the higher-level API used by application code to build
 * prompts, provide user messages, call the model, and retrieve responses.
 */
@Configuration
public class SupportChatConfig {

    /**
     * System prompt defining how the AI assistant should behave.
     *
     * It establishes the assistant's identity, tone, and accuracy rules.
     * It guides the model's responses but does not guarantee that every
     * response will follow these rules perfectly.
     */
    static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Be concise, friendly, and factual. If you don't know something, say so
            instead of guessing. Do not make up order numbers, refund amounts, or
            policy details that were not given to you.
            """;

    /**
     * Creates a reusable ChatClient bean managed by Spring.
     *
     * @param chatModel the ChatModel supplied by Spring Boot auto-configuration
     * @return a ChatClient configured with Aria's default system prompt
     */
    @Bean
    ChatClient supportChatClient(ChatModel chatModel) {

        // Create a ChatClient builder using the configured chat model.
        return ChatClient.builder(chatModel)

                // Add Aria's system instructions to requests made by this client.
                .defaultSystem(SYSTEM_PROMPT)

                // Build and return the configured ChatClient.
                .build();
    }
}
