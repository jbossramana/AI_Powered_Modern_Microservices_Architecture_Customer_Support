package com.example.sarvam.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SarvamChatService {
    private final ChatClient chatClient;
    private final String modelName;

    public SarvamChatService(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.ollama.chat.options.model}") String modelName) {
        this.chatClient = chatClientBuilder.build();
        this.modelName = modelName;
    }

    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    public String modelName() {
        return modelName;
    }
}
