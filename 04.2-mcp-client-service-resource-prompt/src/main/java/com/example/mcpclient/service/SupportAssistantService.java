package com.example.mcpclient.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SupportAssistantService {

    private final ChatClient chatClient;
    private final McpAccessService mcpAccessService;

    public SupportAssistantService(
            ChatClient.Builder chatClientBuilder,
            McpAccessService mcpAccessService) {
        this.chatClient = chatClientBuilder.build();
        this.mcpAccessService = mcpAccessService;
    }

    public String answerWithTools(String question) {
        return chatClient.prompt()
                .system("""
                        You are a customer-support assistant.
                        Use the available MCP tools when live billing or ticket data is needed.
                        Never invent transaction or ticket details.
                        A refund request created by the demo tool is PENDING_REVIEW;
                        never say that money has been refunded.
                        """)
                .user(question)
                .tools(mcpAccessService.toolCallbackProvider())
                .call()
                .content();
    }

    public String draftRefundResponse(
            String customerName,
            String customerIssue) {

        // Fetch the reusable prompt template from the MCP server.
        var promptResult = mcpAccessService.getPrompt(
                "refund-response",
                Map.of(
                        "customerName", customerName,
                        "customerIssue", customerIssue
                )
        );

        String promptText = promptResult.messages().stream()
                .map(message -> String.valueOf(message.content()))
                .reduce("", (left, right) -> left + "\n" + right);

        // Read the policy resource from the MCP server.
        var resourceResult = mcpAccessService.readResource(
                "support://policies/refund"
        );

        String resourceText = resourceResult.contents().toString();

        return chatClient.prompt()
                .system("""
                        Follow the supplied refund policy. Do not invent facts.
                        Do not promise a refund before billing verification.
                        """)
                .user("""
                        MCP prompt template:
                        %s

                        MCP resource content:
                        %s
                        """.formatted(promptText, resourceText))
                .call()
                .content();
    }
}
