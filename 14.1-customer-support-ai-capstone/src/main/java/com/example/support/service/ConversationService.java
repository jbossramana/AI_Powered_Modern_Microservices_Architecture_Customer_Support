package com.example.support.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {
    private final ChatClient chatClient;
    private final Map<String, ArrayDeque<String>> conversations = new ConcurrentHashMap<>();

    public ConversationService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String chat(String conversationId, String message) {
        if (conversationId == null || conversationId.isBlank() ||
                message == null || message.isBlank()) {
            throw new IllegalArgumentException("conversationId and message are required.");
        }
        if (message.length() > 8000) {
            throw new IllegalArgumentException("Message exceeds the 8000 character demo limit.");
        }

        ArrayDeque<String> history = conversations.computeIfAbsent(
                conversationId, ignored -> new ArrayDeque<>());
        String previous;
        synchronized (history) {
            previous = String.join("\n", history);
        }

        String answer = chatClient.prompt()
                .system("You are a helpful customer support assistant. Do not invent order or policy facts.")
                .user("Conversation history:\n" + (previous.isBlank() ? "(none)" : previous)
                        + "\n\nCurrent user message:\n" + message)
                .call()
                .content();

        synchronized (history) {
            history.addLast("User: " + message + "\nAssistant: " + answer);
            while (history.size() > 6) history.removeFirst();
        }
        return answer;
    }
}
