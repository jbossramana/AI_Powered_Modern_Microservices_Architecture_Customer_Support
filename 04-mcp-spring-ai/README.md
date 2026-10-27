# Module 6 — Model Context Protocol (MCP) with Spring AI

A runnable learning example with two independent Spring Boot applications. The **MCP server** publishes enterprise customer, ticket and SLA capabilities as discoverable MCP tools. The **AI client** connects over HTTP/SSE, discovers those remote tools, and makes them available to an Ollama-backed `ChatClient`.

## Implemented concepts

- MCP server/client separation and tool discovery
- Tool metadata: names, descriptions and typed input schemas generated from `@Tool` / `@ToolParam`
- HTTP/SSE MCP transport (server default endpoint `/sse`; client connection base URL points to port 8081)
- Customer, ticket history, ticket-by-ID, SLA policy, and combined support-snapshot tools
- LLM-driven remote tool selection and grounded answers
- Input validation on the chat request, normalized identifiers, not-found responses, and server-side invocation audit logs
- Configurable Ollama URL/model and MCP server URL via environment variables

> Demo data is in memory and deliberately small. It is for education, not production customer data. Production systems should add authentication/authorization, tenant isolation, data minimization, rate limits, durable storage, secret management, tracing/metrics, and audited access policies.

## Prerequisites

- JDK 21
- Maven 3.9+
- Ollama installed and running
- Pull a chat model, for example:

```bat
ollama pull llama3.2:latest
```

The model name can be changed using `OLLAMA_CHAT_MODEL`. The MCP server does not need an LLM; only the client calls Ollama.

## Start the applications (Windows CMD)

Open two separate CMD terminals from the extracted `04-mcp-spring-ai` folder.

**Terminal 1 — MCP server (port 8081):**

```bat
cd mcp-server
mvn spring-boot:run
```

Wait for the server to finish starting before starting the client.

**Terminal 2 — MCP client (port 8080):**

```bat
cd mcp-client
mvn spring-boot:run
```

If needed, set configuration before launching:

```bat
set MCP_SERVER_URL=http://localhost:8081
set OLLAMA_BASE_URL=http://localhost:11434
set OLLAMA_CHAT_MODEL=llama3.2:latest
mvn spring-boot:run
```

## Test URLs and requests

### 1. Ask a question that requires enterprise tools

**URL:** `POST http://localhost:8080/api/chat`

```bat
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"For customer C-1002, summarize the account, list their tickets, and explain the applicable SLA. Use the enterprise tools and distinguish ticket status from SLA commitments.\"}"
```

### 2. Find a specific ticket

```bat
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Look up ticket TCK-1003. Tell me its status, priority, and customer ID using the ticket system.\"}"
```

### 3. Compare customer plan SLAs

```bat
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Compare the SLA response targets for the Pro and Business plans. Retrieve the policy rather than guessing.\"}"
```

### 4. Test unknown IDs and grounding

```bat
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Find customer C-9999 and report the account details.\"}"
```

Expected behavior: the tools report that the customer is not found; the assistant should not invent account data. Exact natural-language output varies by model.

### 5. Validate required input

```bat
curl.exe -i -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\" \"}"
```

The request is expected to fail validation (HTTP 400 when validation is applied by the framework).

### 6. MCP transport endpoint

- Server base URL: `http://localhost:8081`
- SSE endpoint configured by the Spring AI MCP server starter: `http://localhost:8081/sse`
- Client MCP connection: `spring.ai.mcp.client.sse.connections.enterprise-systems.url`

The SSE endpoint is an MCP protocol endpoint, **not** a normal browser page or a REST endpoint that returns the tool list as JSON. Use an MCP-compatible client or the Spring AI client to perform protocol initialization and tool discovery. Check server logs for tool invocation records when calling `/api/chat`.

## Troubleshooting

- Start the server first. The client connects to it during startup.
- Verify Ollama responds and the configured model is available: `ollama list`.
- If the model is missing, run `ollama pull llama3.2:latest` or set `OLLAMA_CHAT_MODEL` to an installed model.
- If MCP connection fails, verify port 8081 is free and `MCP_SERVER_URL` points to the server base URL (not `/sse`).
- If an answer does not call a tool, make the request explicitly ask the assistant to look up the relevant customer, ticket or SLA data. Model tool selection is not perfectly deterministic.

## Project layout

```text
04-mcp-spring-ai/
  mcp-server/   # Enterprise data and MCP tool provider (8081)
  mcp-client/   # Chat REST API and MCP-consuming AI client (8080)
  notes.md      # Detailed concept guide, architecture, trade-offs and labs
```

See [notes.md](notes.md) for the full concept guide and suggested exercises.
