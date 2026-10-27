package com.example.support.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.support.security.GuardedToolCallback;
import com.example.support.security.ToolExecutionPolicy;
import com.example.support.tools.OrderTools;
import com.example.support.tools.PaymentTools;

@Configuration
public class AiConfig {

    @Bean
    ChatClient supportChatClient(
            ChatClient.Builder builder,
            PaymentTools paymentTools,
            OrderTools orderTools,
            ToolExecutionPolicy policy) {

        List<ToolCallback> guardedTools = List.of(
                wrap(ToolCallbacks.from(paymentTools), policy),
                wrap(ToolCallbacks.from(orderTools), policy)
        );

        return builder
                .defaultTools(guardedTools.toArray(new ToolCallback[0]))
                .build();
    }

    private ToolCallback wrap(ToolCallback[] callbacks,
                              ToolExecutionPolicy policy) {
        if (callbacks.length != 1) {
            throw new IllegalStateException(
                    "Expected exactly one tool callback per tool bean");
        }
        return new GuardedToolCallback(callbacks[0], policy);
    }
}
