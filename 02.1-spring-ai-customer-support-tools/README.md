# Customer Support Tool Calling — Spring Boot 4.1 + Spring AI 2.0 + Ollama

Two runnable examples show the **LLM choosing tools** in a customer-support use case.

- **Example 1 — one available tool:** `POST /api/support/account`; only the `AccountLookupTool` bean (with `lookupAccount`) is exposed.
- **Example 2 — multiple available tools:** `POST /api/support/investigate`; the LLM can select `lookupAccount`, `lookupTicket`, and/or `lookupPlan`.

This is a teaching demo with fixed in-memory sample records. It does not connect to real customer systems.

## Requirements

- JDK 21
- Maven 3.9+
- Ollama installed and running
- A tool-capable model pulled locally. Start with `qwen2.5:7b`; if your machine is smaller, try a smaller tool-capable model and verify its tool-call support.

## 1. Start Ollama

In a Windows Command Prompt:

```bat
ollama pull qwen2.5:7b
ollama serve
```

If Ollama is already running as a Windows service/app, you may not need to run `ollama serve` again. Check:

```bat
ollama list
curl.exe http://localhost:11434/api/tags
```

## 2. Start the Spring Boot application

Open Command Prompt in the unzipped project folder:

```bat
mvnw.cmd spring-boot:run
```

If the Maven wrapper is not included in your environment, use:

```bat
mvn spring-boot:run
```

The application listens on `http://localhost:8080`.

Health check:

```bat
curl.exe http://localhost:8080/actuator/health
```

## 3. Example 1 — one tool exposed

Ask about a known customer:

```bat
curl.exe -X POST "http://localhost:8080/api/support/account" -H "Content-Type: application/json" -d "{\"question\":\"Please check the account status and plan for customer CUST-1001.\"}"
```

Try an unknown customer:

```bat
curl.exe -X POST "http://localhost:8080/api/support/account" -H "Content-Type: application/json" -d "{\"question\":\"Look up customer CUST-9999 and tell me the account status.\"}"
```

Only the `AccountLookupTool` bean, containing `lookupAccount`, is made available in this endpoint. The model may decide whether to call it, and supplies the tool arguments. The Java method performs the lookup and returns its result.

## 4. Example 2 — multiple tools exposed

Ticket + plan question:

```bat
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"question\":\"For customer CUST-1001, check ticket TICK-501 and explain whether Premium support includes priority handling.\"}"
```

Ticket-only question:

```bat
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"question\":\"What is the latest status of ticket TICK-502?\"}"
```

Account + plan question:

```bat
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"question\":\"Check customer CUST-1002 and explain the response target for their Basic plan.\"}"
```

The LLM chooses relevant tools from the four methods offered (`lookupAccount`, `lookupTicket`, and `lookupPlan`; account lookup is a separate bean but only one method is present in that bean). Depending on the model and wording, it may call one, several sequentially, or none. The final response is generated after tool results are returned to the model.

## 5. Understand the communication

The logical tool-calling round trip looks like this:

1. **Spring AI → Ollama:** system instructions + user question + tool definitions (name, description, parameter schema).
2. **Ollama → Spring AI:** an assistant message requesting a tool call, including a tool name and JSON arguments. This is not the tool's result.
3. **Spring AI application:** `ToolCallingAdvisor` resolves the requested name to the Java `@Tool` method and executes it.
4. **Java tool → Spring AI:** the tool returns data (here, a JSON string).
5. **Spring AI → Ollama:** conversation history now includes the tool request and tool result.
6. **Ollama → Spring AI:** natural-language final answer; Spring AI returns it from the endpoint.

Simplified illustrative JSON (exact fields and envelopes vary by Ollama/model/version):

**Tool definitions sent with the request**
```json
{
  "tools": [{
    "type": "function",
    "function": {
      "name": "lookupTicket",
      "description": "Look up the status and latest update for a support ticket using its ticket ID",
      "parameters": {
        "type": "object",
        "properties": { "ticketId": { "type": "string" } },
        "required": ["ticketId"]
      }
    }
  }]
}
```

**Model's tool-call decision**
```json
{
  "message": {
    "role": "assistant",
    "tool_calls": [{
      "function": {
        "name": "lookupTicket",
        "arguments": { "ticketId": "TICK-501" }
      }
    }]
  },
  "done": true
}
```

**Tool result sent back as conversation context**
```json
{
  "role": "tool",
  "name": "lookupTicket",
  "content": "{\"ticketId\":\"TICK-501\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"lastUpdate\":\"Engineer investigating login failures\"}"
}
```

These are teaching examples, not captured raw payloads. Do not expect every line to appear exactly like this in logs.

## 6. Read the logs

The project enables Spring AI DEBUG logs and prompt/completion observation logging in `application.yml`. Application logs also mark the tool execution and its result.

Look for these landmarks:

```text
=== EXAMPLE 2: MULTIPLE TOOLS REQUEST ===
USER QUESTION: ...
[Spring AI DEBUG / Ollama client logs]
[TOOL EXECUTION] lookupTicket arguments={"ticketId":"TICK-501"}
[TOOL RESULT] lookupTicket result={"ticketId":"TICK-501",...}
[TOOL EXECUTION] lookupPlan arguments={"planName":"Premium"}
[TOOL RESULT] lookupPlan result=...
FINAL ANSWER: ...
```

The exact Spring AI debug messages and whether every request/response body is printed depends on the client implementation and dependency versions. `org.springframework.ai=DEBUG` is useful, but **DEBUG logging is not a guarantee that the complete raw HTTP JSON body is printed**.

For deeper diagnostics, run with a local HTTP debugging proxy configured between the app and Ollama, or instrument the HTTP client used by the resolved Ollama starter version. Keep such capture local. The Ollama API is at `http://localhost:11434`; avoid exposing it on an untrusted network.

### Sensitive data warning

Prompt/completion logging can print customer text and model responses. Tool logs in this demo print tool arguments and results. Do not enable content logging in production without masking, access control, retention rules, and approval.

## 7. Key concepts

- **LLM:** interprets the user's natural-language request and decides whether a tool is relevant.
- **Tool definition:** description + method name + input schema made available to the model.
- **Tool call:** the model's structured request to invoke a named function; it is not direct execution.
- **Tool execution:** Spring AI invokes the matching Java method. The application remains responsible for authentication, authorization, validation, and side effects.
- **Tool result:** data returned by Java and supplied back to the model as conversation context.
- **Final answer:** the LLM turns the tool result into a user-friendly explanation.
- **Single-tool vs multi-tool:** the first endpoint exposes one named tool; the second exposes the account, ticket, and plan tools. “Multiple tools” does not mean all tools must run.
- **Tool-calling loop in Spring AI 2.0:** `ChatClient` uses the `ToolCallingAdvisor` loop; do not add a manual loop for this normal use case. The loop repeats as needed until the model produces a response without tool calls.
- **Not automatically agentic:** a tool-enabled LLM can choose tools, but this small request/response example does not implement durable planning, long-running goals, memory, approval workflows, or independent scheduling.

## 8. Important implementation notes

- Sample IDs/data are hard-coded to make the example easy to run.
- Use only a model that supports tool calling reliably. Local models can vary in tool-call quality; if the model answers from guesswork instead of calling a tool, try a different tool-capable model.
- Tool names and descriptions strongly influence selection. Use clear, specific descriptions.
- Never trust model-provided arguments as authorization. Validate customer access on the server, not in the prompt.
- This sample uses synchronous HTTP endpoints and a local Ollama instance.
- Versions requested: Spring Boot `4.1.0`, Spring AI `2.0.0`, Java `21`.

## References

- Spring AI tool calling: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI observability: https://docs.spring.io/spring-ai/reference/observability.html
- Spring AI 2.0 GA announcement: https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/
