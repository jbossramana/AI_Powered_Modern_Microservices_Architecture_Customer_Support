package com.example.support.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Step 1: basic customer-support chat. This version only answers questions;
 * it does not access customer records, create tickets, or perform transactions.
 */
@RestController
@RequestMapping("/support")
public class CustomerSupportChatController {

    private final ChatClient chatClient;

    public CustomerSupportChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                    You are a helpful customer support assistant for an enterprise.
                    Be polite, clear, concise, and empathetic.
                    Help with general questions about orders, payments, refunds,
                    login problems, subscriptions, and product usage.
                    Never invent account-specific details, claim that a refund or
                    account change has been completed, or request passwords, OTPs,
                    full card numbers, or other secrets.
                    If a request requires private account data or an action, explain
                    that an authenticated support agent or secure system is needed.
                    If company policy is not provided, say that you cannot confirm it.
                    """)
                .build();
    }

    /**
     * Windows CMD:
     * curl.exe --get "http://localhost:8080/support/ask" --data-urlencode "question=I was charged twice for my order. What should I do?"
     */
    @GetMapping("/ask")
    public Map<String, String> ask(@RequestParam String question) {
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();
        return Map.of(
                "question", question,
                "answer", answer == null ? "" : answer,
                "useCase", "Customer Support",
                "framework", "Spring AI"
        );
    }
}
