# Customer Support: Tools vs Manual-Loop Agent

**Stack:** Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Ollama.

This project has three endpoints for comparing tool calling approaches:

1. `POST /api/support/account` — a ChatClient request with one account tool exposed.
2. `POST /api/support/investigate` — a ChatClient request with multiple support tools exposed.
3. `POST /api/support/agent/ask` — a manual-loop agent. Java explicitly checks for tool calls, executes them, adds tool results to the conversation, and calls the LLM again.

The demo uses in-memory sample data only.

## Prerequisites

- JDK 21
- Maven 3.9+
- Ollama installed and running

In Windows Command Prompt:

```bat
ollama pull qwen2.5:7b
ollama serve
```

If Ollama is already running, do not start a second server. In the project directory run:

```bat
mvn clean spring-boot:run
```

## Try the manual-loop agent

```bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"Check ticket TICK-501 and explain the Premium support response target for customer CUST-1001.\"}"
```

More examples:

```bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"What is the status of ticket TICK-502?\"}"
```

```bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"Check customer CUST-1002 and explain the support response target for their Basic plan.\"}"
```

## Compare with the other endpoints

One tool available:

```bat
curl.exe -X POST "http://localhost:8080/api/support/account" -H "Content-Type: application/json" -d "{\"question\":\"Check the account status and plan for customer CUST-1001.\"}"
```

Multiple tools available:

```bat
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"question\":\"Check ticket TICK-501 and explain the Premium support response target.\"}"
```

## Manual-loop agent: what Java does

The core logic in `AgenticSupportService.ask()` is:

1. Build a `Prompt` with the system message, customer question, and tool-calling options.
2. Call `chatModel.call(prompt)`.
3. Check `response.hasToolCalls()`.
4. If true, call `toolCallingManager.executeToolCalls(prompt, response)`.
5. Take `result.conversationHistory()` and create the next `Prompt`.
6. Call the model again.
7. Stop when there are no tool calls or the maximum iteration count is reached.

The loop is deliberately capped at five iterations. Increase it only when you understand the latency, cost, and safety implications.

## Logs to watch

The service writes markers such as:

```text
[AGENT] Starting manual execution loop
[AGENT] Iteration 1 of 5
[AGENT] LLM requested tool call(s): [lookupTicket]
[TOOL EXECUTION] lookupTicket arguments=...
[TOOL RESULT] lookupTicket result=...
[AGENT] Iteration 2 of 5
[AGENT] Model returned final answer; loop finished.
```

The exact number of iterations and tools selected depends on the model's response. Spring AI DEBUG logging is enabled in `application.yml`. It does not guarantee that every raw HTTP JSON request and response body will be printed; the README's JSON examples are illustrative.

## Difference: automatic loop vs manual loop

| Feature | ChatClient tool example | Manual-loop agent |
|---|---|---|
| Tool selection | LLM | LLM |
| Tool execution | Spring AI's `ToolCallingAdvisor` loop | `ToolCallingManager` called by your Java loop |
| Continue/stop decision | Spring AI checks for more tool calls | Your code checks `response.hasToolCalls()` |
| Iteration cap in this sample | Framework behavior/configuration | Explicitly five iterations |
| Best use | Normal app requests | Custom control, auditing, approval gates, per-step decisions |

This is a **less autonomous, application-controlled agent**. The model decides which tool to request, but Java owns the loop and maximum iteration count. It is not a fully autonomous long-running agent: it has no durable goals, scheduling, or persistent planning.

## Jackson note

The manual-loop service does not require an `ObjectMapper` constructor parameter. If your wider application uses one elsewhere, make sure the Jackson dependency and Boot auto-configuration are present. Avoid creating multiple mapper beans unless you have a specific reason.

## Security notes

- The sample customer/ticket data is hard-coded.
- Never treat an LLM tool call as authorization. Enforce customer permissions in the Java service.
- Validate tool inputs and audit side effects.
- Do not log real customer data or raw prompts in production without redaction and access controls.

## References

- Spring AI tool calling: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI manual tool-calling loop: https://docs.spring.io/spring-ai/reference/2.1/api/tools/chatmodel-tool-calling.html
