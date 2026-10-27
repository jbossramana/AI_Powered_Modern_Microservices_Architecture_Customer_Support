package com.acmesupport.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 0a: the simplest possible Spring AI application.
 *
 * <p>One dependency ({@code spring-ai-starter-model-ollama}) is enough to get a fully
 * autoconfigured {@link org.springframework.ai.chat.model.ChatModel} wired to a local
 * Ollama server. Everything else in this module is just plumbing to expose it over HTTP.
 */
@SpringBootApplication
public class ChatSpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatSpringAiApplication.class, args);
    }
}
