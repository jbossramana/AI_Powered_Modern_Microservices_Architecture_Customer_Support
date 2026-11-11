
package com.acmesupport.mcpclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpChatConfig {

    private static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.

            CRITICAL RULES:
            1. For any request about a customer account, tickets, or SLA,
               you MUST use the available enterprise tools.
            2. For a complete customer summary, call
               getCustomerSupportSnapshot with the supplied customer ID.
            3. Never invent customer names, ticket IDs, ticket statuses,
               storage limits, response times, or SLA commitments.
            4. Use only the information returned by the tools.
            5. If a tool fails or returns no data, clearly say that the
               information could not be retrieved.
            6. Distinguish current ticket status from SLA commitments.
            """;

    @Bean
    ChatClient supportChatClient(
            ChatModel chatModel,
            ToolCallbackProvider mcpToolCallbackProvider) {

        // Retrieve tool callbacks discovered by the MCP client.
        ToolCallback[] callbacks =
                mcpToolCallbackProvider.getToolCallbacks();

        // Display the total number of discovered tools.
        System.out.printf(
                "%n========== MCP TOOL DISCOVERY ==========%n");
        System.out.printf(
                "Total tools retrieved: %d%n", callbacks.length);

        // Display each tool's name and description.
        for (int i = 0; i < callbacks.length; i++) {
            ToolCallback callback = callbacks[i];

            System.out.printf(
                    "%nTool %d:%n", i + 1);
            System.out.printf(
                    "  Name        : %s%n",
                    callback.getToolDefinition().name());
            System.out.printf(
                    "  Description : %s%n",
                    callback.getToolDefinition().description());
        }

        System.out.printf(
                "%n========== END TOOL DISCOVERY ============%n%n");

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(mcpToolCallbackProvider)
                .build();
    }
}
