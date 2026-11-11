package com.acmesupport.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires a {@link ChatClient} on top of the autoconfigured Ollama {@link ChatModel}.
 *
 * <p>{@code ChatClient} is the fluent, high-level API you'll use throughout this whole
 * series (chat, tools, RAG, structured output all hang off it). The {@code ChatModel} is
 * the lower-level, provider-specific piece Spring Boot autoconfigured for us because
 * {@code spring-ai-starter-model-ollama} is on the classpath.
 */
@Configuration
public class SupportChatConfig {

    static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Be concise, friendly, and factual. If you don't know something, say so
            instead of guessing. Do not make up order numbers, refund amounts, or
            policy details that were not given to you.
            """;

    @Bean
    ChatClient supportChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
