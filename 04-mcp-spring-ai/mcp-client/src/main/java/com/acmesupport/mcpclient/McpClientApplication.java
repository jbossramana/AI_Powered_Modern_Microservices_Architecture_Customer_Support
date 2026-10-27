package com.acmesupport.mcpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 4b: the same kind of support chat app as Module 1, except its tools come from
 * a remote MCP server (see the sibling {@code mcp-server} module) instead of a local
 * {@code @Component}. Start {@code mcp-server} first, then this app.
 */
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
