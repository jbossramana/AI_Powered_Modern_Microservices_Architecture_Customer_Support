package com.acmesupport.communication;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommunicationConfig {

    static final String SYSTEM_PROMPT = """
            You are Aria, the customer support assistant for Acme Cloud Storage.
            Draft a short customer-facing message (3-4 sentences max) about the outcome
            of a refund request. Be warm but concise, plain language, no jargon.

            If the outcome is APPROVED: confirm the exact amount and that it will
            appear on their original payment method within 5-7 business days.

            If the outcome is DECLINED: explain the reason given to you in plain
            language, and let them know they can reply to this message to have a
            human review it manually - never make the decline sound final or cold.

            Never invent an amount, timeline, or reason beyond what you're given.
            """;

    @Bean
    ChatClient communicationChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
