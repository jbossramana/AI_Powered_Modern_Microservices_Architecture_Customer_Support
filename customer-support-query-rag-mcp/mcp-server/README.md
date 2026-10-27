# Spring AI MCP Customer Support Demo

Two Spring Boot applications demonstrate an MCP server and an MCP client with LLM-driven tool calling.

- `mcp-server` (port 8081): exposes `getOrderStatus` and `getTicketDetails` over MCP Streamable HTTP.
- `mcp-client` (port 8080): connects to the MCP server and supplies its discovered tools to Spring AI `ChatClient`.
- Ollama provides the local chat model.

## Requirements
- JDK 21+
- Maven 3.9+
- Ollama installed and running at `http://localhost:11434`

Uses Spring Boot 4.1.0 and Spring AI 2.0.1. This source package has not been built in your environment; verify exact patch-version availability and compatibility.

## 1. Download model (Windows CMD)
```bat
ollama pull qwen2.5:7b
ollama list
```

## 2. Start MCP server (CMD window 1)
```bat
cd spring-ai-mcp-customer-support\mcp-server
mvn spring-boot:run
```
MCP endpoint: `http://localhost:8081/mcp`.

## 3. Start MCP client (CMD window 2)
```bat
cd spring-ai-mcp-customer-support\mcp-client
mvn spring-boot:run
```

## 4. Test (CMD window 3)
```bat
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"Where is order ORD-1001?\"}"
```
```bat
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"Please check ticket TKT-2001\"}"
```

## Request flow
1. Client receives a customer question.
2. Spring AI MCP client discovers tools exposed by the configured server.
3. The LLM selects a tool when needed.
4. Spring AI routes the tool call through MCP to the server.
5. The server executes the Java method and returns the result.
6. The LLM forms the final answer.

## Important notes
- MCP is a protocol, not an LLM or an agent by itself.
- Sample order/ticket records are hard-coded demonstration data, not a live business system.
- The HTTP MCP endpoint is unauthenticated in this demo. Keep it local and secure it before network exposure.
- `curl` tests the client's REST endpoint. The MCP server uses MCP Streamable HTTP, not a custom REST endpoint per tool.
