package com.example.support.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final ChatClient chatClient;

    public SupportController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/{orderId}")
    public String investigate(
            @PathVariable String orderId,
            @RequestBody SupportRequest request) {

        return chatClient.prompt()
                .system("""
                    You are a customer support investigation agent.

                    You have access to these tools:
                    1. checkPaymentStatus
                    2. checkOrderStatus

                    Investigation rules:
                    - You MUST call checkPaymentStatus for the given order ID.
                    - You MUST call checkOrderStatus for the given order ID.
                    - Base your final answer only on the actual tool results.
                    - Do not report a tool result until you have received
                      its actual execution result.
                    - After receiving both results, explain the findings.
                    - Never invent tool results.
                    """)
                .user("""
                    Order ID: %s
                    Customer question: %s
                    """.formatted(orderId, request.question()))
                .call()
                .content();
    }
    public record SupportRequest(String question) {}
}
