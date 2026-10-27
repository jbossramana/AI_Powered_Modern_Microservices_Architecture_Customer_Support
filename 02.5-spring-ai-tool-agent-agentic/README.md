# Customer Support AI Patterns — Spring Boot 4.1 + Spring AI 2.0 + Ollama

A runnable teaching project showing:
1. Single tool without an agent
2. Multiple tools without an agent
3. Single tool with an agent
4. Multiple tools with an agent
5. Agentic-style workflow with one tool
6. Agentic-style workflow with multiple tools
7. Multi-agent collaboration

The app uses in-memory demo data, so you can learn without a database. Replace `SupportDataService` with repositories or downstream service clients in a real application.

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Ollama
- Default model: `llama3.2:latest`
- Maven

Spring AI 2.0 uses the Spring Boot 4 line. Spring AI 2.0's `ChatClient` tool-calling advisor manages the tool execution loop. Direct `ChatModel` calls do not automatically execute tool calls in Spring AI 2.x.

## Teaching tip

For a classroom explanation, read `CONCEPTS.md` from the **Instructor's Teaching Guide** section first. It uses one customer-support problem to explain Java-only workflows, LLM-assisted no-agent workflows, agents, agentic workflows, and multi-agent collaboration in a progressive order. It also includes a short classroom script and quick-check questions.

## 1. Install and start Ollama

Install Ollama from https://ollama.com/download, then in Windows CMD run:

```cmd
ollama pull llama3.2
ollama list
```

Make sure Ollama is running. Its default endpoint is `http://localhost:11434`.

If you use a different model, set `OLLAMA_CHAT_MODEL` to the exact model name.

## 2. Run the application

From the project root:

```cmd
mvn clean spring-boot:run
```

Health check:

```cmd
curl.exe "http://localhost:8080/actuator/health"
```

The first LLM-backed request may take time while Ollama loads the model.

## 3. Test with Windows CMD curl.exe

Run each command in **Command Prompt**. `curl.exe` is explicit, and JSON double quotes are escaped for CMD.

### A. One tool, no agent — Java calls a service directly

```cmd
curl.exe -X POST "http://localhost:8080/demo/no-agent/single-tool" -H "Content-Type: application/json" -d "{\"customerId\":\"C-1001\"}"
```

Expected core fields:
```json
{"pattern":"NO_AGENT_SINGLE_TOOL","customerId":"C-1001","plan":"Pro","calledTools":["getCustomerPlan"]}
```

### B. Multiple tools, no agent — Java owns the sequence

```cmd
curl.exe -X POST "http://localhost:8080/demo/no-agent/multiple-tools" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-1001\",\"customerId\":\"C-1001\"}"
```

Java calls ticket status, customer plan, and support policy in a fixed sequence.

### B2. Single tool, no agent, but use the LLM to explain the result

Java decides to call `getCustomerPlan`, executes it, and passes the verified result to the LLM. The LLM only explains the result; it does not choose or call tools.

```cmd
curl.exe -X POST "http://localhost:8080/demo/no-agent/single-tool-with-llm" -H "Content-Type: application/json" -d "{\"customerId\":\"C-1001\"}"
```

### B3. Multiple tools, no agent, but use the LLM to summarize

Java calls ticket, plan, and policy services in a fixed sequence. It then passes the verified facts to the LLM to produce a readable summary. The LLM does not select or execute tools.

```cmd
curl.exe -i -X POST "http://localhost:8080/demo/no-agent/multiple-tools-with-llm" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-1001\",\"customerId\":\"C-1001\"}"
```

### C. Single tool with an agent — LLM may choose plan lookup

```cmd
curl.exe -X POST "http://localhost:8080/demo/agent/single-tool" -H "Content-Type: application/json" -d "{\"question\":\"What plan does customer C-1001 have? Use the customer plan tool.\"}"
```

### D. Multiple tools with an agent — LLM chooses relevant tools

```cmd
curl.exe -X POST "http://localhost:8080/demo/agent/multiple-tools" -H "Content-Type: application/json" -d "{\"question\":\"Check ticket TCK-1001 and explain which support policy applies to customer C-1001.\"}"
```

### E. Agentic-style workflow with one tool

```cmd
curl.exe -X POST "http://localhost:8080/demo/agentic/single-tool" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-1001\",\"customerId\":\"C-1001\",\"goal\":\"Find the current ticket status and summarize it.\"}"
```

Only the ticket-status tool is exposed in this example. If one lookup answers the goal, a more elaborate loop may not be necessary.

### F. Agentic-style workflow with multiple tools

```cmd
curl.exe -X POST "http://localhost:8080/demo/agentic/multiple-tools" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-1001\",\"customerId\":\"C-1001\",\"goal\":\"Investigate a possible duplicate payment and explain the safe next step.\"}"
```

The model can choose from ticket, plan, policy, order, and payment read tools. It should state what still needs verification. The `WorkflowService.agenticMultipleTools()` method now contains an explicit Java `while` loop using `ChatModel` and `ToolCallingManager`: Java checks for tool calls, executes requested Java tool methods, adds their results to conversation history, and calls the model again. The loop is bounded to five tool-call rounds. See `AGENTIC_LOOP_EXPLAINED.md` for the code walkthrough.

### G. Multi-agent workflow — triage, investigation, response

```cmd
curl.exe -X POST "http://localhost:8080/demo/multi-agent/support" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-1001\",\"customerId\":\"C-1001\",\"message\":\"I was charged twice for the same order. Please help.\"}"
```

The Java orchestrator invokes:
- `TriageAgent`: classifies the issue.
- `InvestigationAgent`: uses read-only tools to gather facts.
- `ResponseAgent`: drafts a customer-facing answer from the notes.

## 4. API reference

| Endpoint | Workflow |
|---|---|
| `POST /demo/no-agent/single-tool` | Java directly calls one service |
| `POST /demo/no-agent/multiple-tools` | Java calls several services in fixed order |
| `POST /demo/no-agent/single-tool-with-llm` | Java calls one service, then LLM explains result; no agent |
| `POST /demo/no-agent/multiple-tools-with-llm` | Java calls several services, then LLM summarizes; no agent |
| `POST /demo/agent/single-tool` | LLM can choose one tool |
| `POST /demo/agent/multiple-tools` | LLM chooses among several tools |
| `POST /demo/agentic/single-tool` | Goal-oriented prompt with one available tool |
| `POST /demo/agentic/multiple-tools` | Goal-oriented prompt with multiple read tools |
| `POST /demo/multi-agent/support` | Java coordinates three specialist LLM roles |

## 5. Demo data

- `C-1001` → Pro
- `C-1002` → Business
- `TCK-1001` → OPEN, BILLING
- `TCK-1002` → IN_PROGRESS, LOGIN
- `TCK-1003` → RESOLVED, DELIVERY

Other IDs return default demo values. Edit `SupportDataService` to add more cases.

## 6. Code map

- `CustomerSupportAiApplication.java` — application entry point
- `api/DemoController.java` — REST endpoints
- `api/ApiModels.java` — validated request records
- `domain/SupportDataService.java` — deterministic in-memory facts
- `tools/SupportTools.java` — Spring AI `@Tool` methods
- `service/WorkflowService.java` — seven patterns and agent orchestration
- `application.yml` — model and server configuration

## 7. Important concept notes

- A **tool** is a capability, usually backed by a Java method.
- **Without an agent**, Java decides which service to call and in what order.
- An **agent** lets the LLM choose from tools made available to it.
- **Agentic AI** emphasizes bounded goal pursuit: plan, act, observe, adapt, stop.
- The agentic endpoint demonstrates the loop using Spring AI's `.tools(...).call()` integration; it does not hand-code `hasToolCalls()` or tool execution.
- **Multi-agent** means multiple specialist agents collaborate under orchestration.
- These terms overlap in industry usage; see `CONCEPTS.md` for detailed distinctions.

## 8. Safety notes

This is a learning demo, not a production-ready customer support system.

- Data is in memory and not a real database.
- No real refund/cancellation action is implemented.
- Do not expose destructive tools without authorization, validation, approval, idempotency, and audit logs.
- Validate all tool inputs; treat ticket text as untrusted.
- Apply mandatory business policy in Java, not only in prompts.
- Use timeouts and tool-call/iteration limits in production.
- The model can return variable text; do not rely on exact wording in tests.
- Model tool calling varies by model. If a local model ignores tool descriptions, try a model with stronger tool-calling support.

## 9. Troubleshooting

**Ollama connection refused**
- Start Ollama and verify `http://localhost:11434`.
- Run `ollama list`.

**Model not found**
```cmd
ollama pull llama3.2
```
Confirm `spring.ai.ollama.chat.model` matches the installed model tag.

**Maven build fails**
- Check Java: `java -version` (Java 21).
- Check Maven: `mvn -version`.
- Ensure Maven Central is reachable and no proxy is blocking dependency downloads.

**Tool not called**
- Ask a direct question that requires the tool and names the needed identifier.
- Review `SupportTools` tool descriptions.
- Enable debug logs:
```yaml
logging:
  level:
    org.springframework.ai: DEBUG
```

## 10. Official references

- Spring AI Getting Started: https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring AI Tool Calling: https://docs.spring.io/spring-ai/reference/api/tools.html
- Spring AI Ollama Chat: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
