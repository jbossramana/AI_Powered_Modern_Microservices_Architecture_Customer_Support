package com.acmesupport.mcpclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class McpChatConfig {

    private static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Use the available tools to look up real customer, ticket, and SLA data
            before answering. Never guess account details or ticket status.
            """;

    @Bean
    ChatClient supportChatClient(
            ChatModel chatModel,
            ToolCallbackProvider mcpToolCallbackProvider) {

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(mcpToolCallbackProvider)
                .build();
    }
}

