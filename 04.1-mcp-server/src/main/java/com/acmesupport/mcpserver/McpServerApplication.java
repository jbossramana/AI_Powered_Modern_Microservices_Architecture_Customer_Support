package com.acmesupport.mcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Step 4a: an MCP server. No REST controllers here on purpose - the entire public
 * surface of this app is the three MCP tools registered below, discoverable and
 * callable by any MCP client that connects, over the Model Context Protocol instead
 * of a bespoke REST contract.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    /**
     * The spring-ai-starter-mcp-server-webmvc auto-configuration detects this
     * ToolCallbackProvider bean at startup and registers every {@code @Tool}-annotated
     * method on EnterpriseTools as an MCP tool automatically - no manual JSON-RPC
     * wiring, no hand-written tool registry.
     */
    @Bean
    ToolCallbackProvider enterpriseToolCallbacks(EnterpriseTools enterpriseTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(enterpriseTools)
                .build();
    }
}
