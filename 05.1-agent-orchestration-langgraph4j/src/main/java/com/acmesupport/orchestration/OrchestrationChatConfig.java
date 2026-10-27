package com.acmesupport.orchestration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrchestrationChatConfig {

    /**
     * One plain ChatClient, shared by TriageNode and ResponseDraftNode. Nothing
     * LangGraph4j-specific about it - it's the same bean shape as every earlier module.
     */
    @Bean
    ChatClient orchestrationChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
