# MCP with Spring AI 2.0.0, Spring Boot 4.1.0, JDK 21 and Ollama

A complete two-application learning project demonstrating MCP **Tools, Resources and Prompts**.

## Versions

- JDK: 21
- Spring Boot: 4.1.0
- Spring AI: 2.0.0
- Ollama: local model runtime
- Default model: `qwen2.5:3b` (configurable)
- Transport: MCP Streamable HTTP
- Build: Maven multi-module

> This project targets the requested Spring Boot 4.1.0 and Spring AI 2.0.0 versions. It is a teaching/demo implementation. The refund tool creates a simulated `PENDING_REVIEW` record only; it does not call a payment provider or move money.

## Project structure

```text
mcp-spring-ai-demo/
|-- pom.xml
|-- mcp-server/
|   |-- pom.xml
|   `-- src/main/
|       |-- java/com/example/mcpserver/
|       |   |-- McpServerApplication.java
|       |   |-- tools/SupportTools.java
|       |   |-- resources/SupportResources.java
|       |   `-- prompts/SupportPrompts.java
|       `-- resources/application.yml
`-- mcp-client/
    |-- pom.xml
    `-- src/main/
        |-- java/com/example/mcpclient/
        |   |-- McpClientApplication.java
        |   |-- controller/McpDemoController.java
        |   |-- controller/SupportController.java
        |   `-- service/
        |       |-- McpAccessService.java
        |       `-- SupportAssistantService.java
        `-- resources/application.yml
```

## MCP concepts in this project

| MCP capability | Server implementation | What it does |
|---|---|---|
| Tool | `@McpTool` in `SupportTools` | Executes an operation, e.g. duplicate-payment check |
| Resource | `@McpResource` in `SupportResources` | Exposes read-only policy documents |
| Prompt | `@McpPrompt` in `SupportPrompts` | Exposes reusable prompt templates |

### Tools included

- `checkDuplicatePayment(customerId, subscriptionId)`
- `getTicket(ticketId)`
- `createRefundRequest(customerId, subscriptionId, reason)`

### Resources included

- `support://policies/refund`
- `support://policies/payment`

### Prompts included

- `refund-response(customerName, customerIssue)`
- `ticket-summary(ticketId, ticketDetails)`

## Prerequisites

Install and verify:

```cmd
java -version
mvn -version
ollama --version
```

Use JDK 21 for Maven and both applications.

Install Ollama from https://ollama.com/download.

Pull a model. `qwen2.5:3b` is the default in this sample:

```cmd
ollama pull qwen2.5:3b
```

If you prefer another locally available model, set `OLLAMA_MODEL` to its exact Ollama tag. Tool-calling capability varies by model; use an Ollama model that supports tool calling for `/api/support/answer`.

Start Ollama if it is not already running:

```cmd
ollama serve
```

If Ollama is already running as a Windows service/app, do not start a second server.

## Build the complete project

Open CMD in the extracted project root (the folder containing the root `pom.xml`):

```cmd
mvn -U clean package
```

If Maven is not recognized, install Maven or use your Maven Wrapper if you add one to the project.

## Run the applications

Open **three separate CMD windows**.

### CMD window 1 — Ollama

```cmd
ollama serve
```

If already running, leave it running.

### CMD window 2 — MCP Server

From the project root:

```cmd
cd mcp-server
mvn spring-boot:run
```

Server endpoint:

```text
http://localhost:8085/mcp
```

### CMD window 3 — MCP Client + Ollama integration

From the project root in another CMD:

```cmd
cd mcp-client
mvn spring-boot:run
```

Client REST API:

```text
http://localhost:8080
```

Wait until both Spring applications report that they have started.

## Test using Windows CMD curl.exe

The examples below are for **Windows Command Prompt (CMD)**. Use `curl.exe`, not an alias.

### 1. Check client health

```cmd
curl.exe -i "http://localhost:8080/actuator/health"
```

### 2. Discover MCP tools, resources and prompts

```cmd
curl.exe -i "http://localhost:8080/api/mcp/catalog"
```

Expected catalog contains the three demo tools, two resources and two prompts. The server may return additional protocol metadata.

The client also prints the discovered capabilities using `System.out.printf()` in its console.

### 3. Read the refund policy resource

```cmd
curl.exe -i "http://localhost:8080/api/mcp/resource?uri=support%3A%2F%2Fpolicies%2Frefund"
```

Read the payment policy resource:

```cmd
curl.exe -i "http://localhost:8080/api/mcp/resource?uri=support%3A%2F%2Fpolicies%2Fpayment"
```

### 4. Retrieve a reusable MCP prompt

```cmd
curl.exe -i "http://localhost:8080/api/mcp/prompt?name=refund-response&customerName=Ravi&customerIssue=I%20was%20charged%20twice"
```

Retrieve the ticket-summary prompt:

```cmd
curl.exe -i "http://localhost:8080/api/mcp/prompt?name=ticket-summary&customerName=Ravi&customerIssue=Ticket%20T1001"
```

Note: the `ticket-summary` prompt requires `ticketId` and `ticketDetails`, not the refund prompt's arguments. For that prompt, use the Java MCP client directly or extend the demo controller with those two request parameters.

### 5. Ask Ollama to draft a refund response using an MCP Prompt and Resource

This endpoint:
1. Requests the `refund-response` prompt from the MCP server.
2. Reads `support://policies/refund` from the MCP server.
3. Passes both to Ollama through Spring AI ChatClient.
4. Returns the generated response.

```cmd
curl.exe -i -X POST "http://localhost:8080/api/support/refund-draft" -H "Content-Type: application/json" -d "{\"customerName\":\"Ravi\",\"customerIssue\":\"I was charged twice for my monthly subscription. Please help me get a refund.\"}"
```

### 6. Ask Ollama a question where it may invoke MCP tools

```cmd
curl.exe -i -X POST "http://localhost:8080/api/support/answer" -H "Content-Type: application/json" -d "{\"question\":\"For customer C1001 and subscription SUB1001, check whether duplicate successful payments exist. Explain the result and do not create a refund automatically.\"}"
```

Try ticket lookup:

```cmd
curl.exe -i -X POST "http://localhost:8080/api/support/answer" -H "Content-Type: application/json" -d "{\"question\":\"Please retrieve ticket T1001 and summarize its status and priority.\"}"
```

The model decides whether to invoke a tool. A tool being discovered and supplied to ChatClient does not guarantee that the model will call it. If the model answers without a tool call, check the model's tool-calling support and inspect the MCP server console.

## Important distinction: Tools vs Resources vs Prompts

### Tool

A tool is an operation. The model can request it, and the MCP client routes the request to the server.

Example: `checkDuplicatePayment`.

### Resource

A resource is information that the client can read using a URI.

Example: `support://policies/refund`.

Resources are not automatically included in every LLM request. The client application chooses when to read a resource and how to pass its content to the model. This sample explicitly reads the refund resource in `/api/support/refund-draft`.

### Prompt

A prompt is a reusable server-side message template with named arguments.

Example: `refund-response`.

A client requests a prompt by name and supplies its arguments. The server returns prompt messages. This sample passes those messages to Ollama through ChatClient.

## Configuration

### MCP Server

`mcp-server/src/main/resources/application.yml`

- Port: `8085`
- Protocol: `STREAMABLE`
- Endpoint: `/mcp`
- Annotation scanning enabled
- Synchronous server

### MCP Client

`mcp-client/src/main/resources/application.yml`

- Port: `8080`
- MCP server base URL: `http://localhost:8085`
- MCP endpoint: `/mcp`
- Client type: `SYNC`
- Ollama base URL: `http://localhost:11434`
- Model: `qwen2.5:3b`

You can override values without editing YAML:

```cmd
set OLLAMA_MODEL=qwen2.5:3b
set OLLAMA_BASE_URL=http://localhost:11434
set MCP_SERVER_URL=http://localhost:8085
```

Then start the client from the same CMD window.

## Troubleshooting

### MCP catalog is empty or client cannot connect

1. Start `mcp-server` first.
2. Confirm it started on port 8085.
3. Confirm the client YAML has `url: http://localhost:8085` and `endpoint: /mcp`.
4. Check that both applications use Spring AI 2.0.0 dependencies.
5. Check the server console for annotation-scanner errors.

### Ollama connection refused

Start Ollama and verify:

```cmd
ollama list
```

### Model not found

Pull the configured model:

```cmd
ollama pull qwen2.5:3b
```

### Model does not invoke tools

Tool calling depends on model capabilities and model behavior. Try a model with Ollama tool-calling support, use an explicit question that requires live ticket or billing data, and inspect the MCP server console. The sample uses `.tools(toolCallbackProvider)` to make the remote MCP tools available to ChatClient.

### Port already in use

Stop the process using port 8080 or 8085, or change the relevant `server.port`.

## Security notes

The demo MCP server is intentionally unauthenticated for localhost learning. Do not expose it to a network or production environment without authentication, authorization, input validation, auditing, and transport security. The refund tool is a simulation only.

## Official references

- Spring AI MCP overview: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- Spring AI MCP server annotations: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html
- Spring AI MCP client starter: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
- Spring AI Ollama chat: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- Spring Boot 4.1.0 release: https://spring.io/blog/2026/06/10/spring-boot-4/
