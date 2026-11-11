package com.example.mcpclient.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;

@Service
public class McpAccessService {

    private final List<McpSyncClient> clients;
    private final ToolCallbackProvider toolCallbackProvider;

    public McpAccessService(
            List<McpSyncClient> clients,
            ToolCallbackProvider toolCallbackProvider) {
        this.clients = clients;
        this.toolCallbackProvider = toolCallbackProvider;
    }

    private McpSyncClient client() {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException(
                    "No MCP Sync Client found. Check the MCP client URL and server startup.");
        }
        return clients.get(0);
    }

    public Map<String, Object> catalog() {
        var mcpClient = client();

        var tools = mcpClient.listTools().tools();
        var resources = mcpClient.listResources().resources();
        var prompts = mcpClient.listPrompts().prompts();

        System.out.printf("%n========== MCP CAPABILITY CATALOG ==========%n");
        System.out.printf("Tools discovered    : %d%n", tools.size());
        tools.forEach(t -> System.out.printf("  TOOL     : %s - %s%n", t.name(), t.description()));

        System.out.printf("Resources discovered: %d%n", resources.size());
        resources.forEach(r -> System.out.printf("  RESOURCE : %s - %s%n", r.uri(), r.name()));

        System.out.printf("Prompts discovered  : %d%n", prompts.size());
        prompts.forEach(p -> System.out.printf("  PROMPT   : %s - %s%n", p.name(), p.description()));

        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        System.out.printf("Tool callbacks available to Spring AI ChatClient: %d%n", callbacks.length);
        System.out.printf("============================================%n%n");

        return Map.of(
                "tools", tools,
                "resources", resources,
                "prompts", prompts,
                "toolCallbackCount", callbacks.length
        );
    }

    public ReadResourceResult readResource(String uri) {
        return client().readResource(
                ReadResourceRequest.builder(uri).build()
        );
    }

    public GetPromptResult getPrompt(String promptName, Map<String, Object> arguments) {
        return client().getPrompt(
                GetPromptRequest.builder(promptName)
                        .arguments(arguments)
                        .build()
        );
    }

    public ToolCallbackProvider toolCallbackProvider() {
        return toolCallbackProvider;
    }
}
