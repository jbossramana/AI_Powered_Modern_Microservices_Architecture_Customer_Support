# Customer Support — Agentic AI (Bounded Autonomous Investigation)

**Stack:** Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Ollama.

This extends the manual-loop agent into a goal-directed agentic workflow. The model can choose a next step, inspect tool results, and choose further steps within one request.

> Safety boundary: this demo has read-only account, ticket, and plan lookup tools. It cannot issue refunds, update tickets, or change accounts. “Autonomous” means it can choose and sequence available read-only investigation steps; Java still enforces the maximum cycle count.

## Run

Prerequisites: JDK 21, Maven 3.9+, Ollama.

```bat
ollama pull qwen2.5:7b
ollama serve
```

If Ollama is already running, skip `ollama serve`. In the extracted project directory:

```bat
mvn clean spring-boot:run
```

## Agentic endpoint

```bat
curl.exe -X POST "http://localhost:8080/api/support/agentic/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate ticket TICK-501 for customer CUST-1001. Check the ticket, account, and relevant support plan; then summarize the situation and any next steps.\"}"
```

Another example:

```bat
curl.exe -X POST "http://localhost:8080/api/support/agentic/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate why customer CUST-1002 may be having account access trouble. Check their account and plan, and report only facts available from the tools.\"}"
```

## Why this is more agentic than a fixed manual loop

A basic manual-loop agent can call a tool and repeat until there are no tool calls. This version adds an explicit **goal-directed policy** to the system instructions:

1. Understand the goal.
2. Decide what information is missing.
3. Select the next tool/action.
4. Read results and reconsider the plan.
5. Continue only if another tool call is useful.
6. Stop when the goal is satisfied, data is missing, or the step budget is exhausted.

The Java loop is still the control mechanism. The LLM decides the next action at each cycle; the application controls which tools exist and how many cycles can run.

## Logs

Look for:

```text
[AGENTIC] Starting goal-directed investigation
[AGENTIC] Decision/action cycle 1/8
[AGENTIC] LLM selected next action(s): [lookupTicket]
[TOOL EXECUTION] lookupTicket arguments=...
[TOOL RESULT] lookupTicket result=...
[AGENTIC] Decision/action cycle 2/8
[AGENTIC] LLM selected next action(s): [lookupAccount]
...
[AGENTIC] LLM decided no further tool action is required.
[AGENTIC] Goal investigation finished.
```

The actual selected tools and cycle count vary by model response. DEBUG logs do not guarantee complete raw HTTP request/response JSON payloads. The application logs tool arguments and results for learning; do not log real customer data in production without redaction and controls.

## Important distinction

- **Tools + LLM:** the model can select a tool; Spring AI handles tool execution iterations.
- **Manual-loop agent:** Java explicitly loops and executes tool calls.
- **This bounded agentic example:** the model is instructed to work toward a goal, choose/revise next actions based on results, and stop when done; Java still runs the loop and imposes limits.

This is not an unlimited autonomous agent: it does not run in the background, schedule its own future work, preserve long-term memory, or act outside the provided tools.

## Production hardening

Before adding write-capable tools, add server-side authorization, input validation, idempotency, audit logs, explicit approval for high-impact actions, timeouts, rate limits, and a strict allowlist. Never use the system prompt as the only security boundary. Validate that a tool's requested customer ID is authorized for the authenticated user.

## Jackson note

This service does not require `ObjectMapper` in its constructor. If your app injects `ObjectMapper` elsewhere, ensure Jackson is present and Boot auto-configuration is active.

## References

- Spring AI tools: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI observability: https://docs.spring.io/spring-ai/reference/observability.html
