# Customer Support AI — Spring Boot 4.1 + Spring AI 2.0 + Ollama

This project demonstrates customer-support examples with a local Ollama model:

- **Agent endpoint**: the LLM can select relevant tool(s) to answer a direct question.
- **Goal-oriented investigation endpoint**: an explicit Java execution loop calls the model again after tool results; Java enforces a maximum of 5 tool rounds.
- Support examples include invoices/billing, ticket/SLA investigation, cloud service health and deployments, storage allowance, and login diagnostics.

The backend tools are intentionally **simulated, read-only demo methods**. They do not connect to a real billing system, cloud monitor, ticket database, or identity service. Replace the method bodies in `SupportTools.java` with secured service/API/database calls before using this architecture in production.

## Versions

- Java 21+
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Ollama local model (default: `llama3.1:8b`)

Spring AI 2.0.0 is aligned with Spring Boot 4.x; see the official [Spring AI 2.0.0 announcement](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/) and [getting started documentation](https://docs.spring.io/spring-ai/reference/getting-started.html).

## 1. Install and start Ollama

Install Ollama from https://ollama.com/download

Open Windows Command Prompt and download a model with tool-calling capability:

```cmd
ollama pull llama3.1:8b
```

Start Ollama (if it is not already running):

```cmd
ollama serve
```

Keep that terminal open. In another terminal, check the model:

```cmd
ollama list
```

If you have a different local model, set `OLLAMA_MODEL` to its exact name. Tool-calling support varies by model; if tools are not called reliably, try another Ollama model that supports tool calling.

## 2. Run the Spring Boot application

From the folder containing `pom.xml`:

```cmd
mvn spring-boot:run
```

This ZIP does not include Maven Wrapper files, so use an installed Maven distribution (`mvn`).

The API listens on `http://localhost:8080`.

## 3. Try the agent endpoint

### A. Latest invoice

```cmd
curl.exe -X POST "http://localhost:8080/api/support/agent" -H "Content-Type: application/json" -d "{\"message\":\"Where can I find my latest invoice for customer C-1001?\"}"
```

Expected behavior: model should call `getLatestInvoice("C-1001")` and explain the returned simulated invoice details.

### B. Ticket status

```cmd
curl.exe -X POST "http://localhost:8080/api/support/agent" -H "Content-Type: application/json" -d "{\"message\":\"What is the current status and priority of ticket TCK-1001?\"}"
```

### C. Service health

```cmd
curl.exe -X POST "http://localhost:8080/api/support/agent" -H "Content-Type: application/json" -d "{\"message\":\"Is the cloud upload service healthy right now? Check service health for cloud-upload-api.\"}"
```

## 4. Try goal-oriented agentic investigations

### A. Investigate a delayed ticket

```cmd
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate why ticket TCK-1001 has been open for several hours. Check status, SLA, and assignment/escalation history, then explain what is known and what needs verification.\",\"customerId\":\"C-1001\",\"ticketId\":\"TCK-1001\"}"
```

Possible tool sequence: `getTicketStatus` → `getTicketSla` → `getTicketHistory`. The model decides which tools to request based on the task and earlier observations.

### B. Investigate possible overcharging

```cmd
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate whether the customer was charged more than invoice INV-1001. Compare the invoice total with transaction records. Do not claim a duplicate captured charge unless the evidence confirms it.\",\"customerId\":\"C-1001\",\"invoiceId\":\"INV-1001\"}"
```

Possible tool sequence: `getInvoiceDetails` → `getTransactionSummary`. A PENDING transaction is not proof that a second charge was captured.

### C. Investigate slowness after deployment

```cmd
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate why the cloud upload API became slow after deployment DEP-2001. Check service health, deployment details, and performance metrics. Separate evidence from possible causes.\",\"customerId\":\"C-1001\"}"
```

Possible tool sequence: `getServiceHealth` → `getDeploymentHistory` → `getPerformanceMetrics`.

### D. Investigate upload failures despite storage being under quota

```cmd
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate why uploads may be failing even though storage usage is below the plan allowance. Check the customer's plan, usage, and relevant service health. Explain the evidence and next step.\",\"customerId\":\"C-1001\"}"
```

### E. Investigate login failures after password reset

```cmd
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"goal\":\"Investigate why customer C-1001 cannot log in after a password reset. Check account/authentication diagnostics and explain what is known. Do not request or reveal passwords.\",\"customerId\":\"C-1001\"}"
```

## 5. List example messages

```cmd
curl.exe "http://localhost:8080/api/support/examples"
```

## 6. What happens inside the application?

### Agent request

1. The REST controller receives the customer message.
2. `SupportAssistantService.answerWithAgent()` sends the message and available tool definitions to the model.
3. The model may request a tool such as `getTicketStatus`.
4. Spring AI invokes the Java method in `SupportTools`.
5. The tool result is sent back to the model.
6. The model produces a natural-language answer.

### Goal-oriented investigation

1. The request includes an investigation goal and identifiers.
2. The model determines which evidence is relevant.
3. It requests one or more read-only tools.
4. Tool observations become part of the conversation.
5. The model can request further checks based on those observations.
6. It returns findings and uncertainty when enough evidence is available.

The `/agent` endpoint uses the framework-managed `ToolCallingAdvisor` loop. The `/investigate` endpoint disables automatic advisor registration for that request and runs an explicit Java `while` loop using `ToolCallingManager`. This makes the tool rounds visible in logs and enforces a maximum of five rounds. The model still chooses the next tool based on the goal and observations; Java controls execution and stopping limits.

The manual loop follows the Spring AI 2.x user-controlled tool-execution pattern. Provider-specific tool-calling behavior can vary by Ollama model, so use a model that supports tool calling and check the logs if no tool calls are returned.

## 7. Agent vs. agentic AI vs. Java/Drools

- **Agent**: LLM selects relevant tool(s) to answer a question.
- **Agentic investigation**: LLM uses observations to decide what to investigate next; Java executes each requested tool and enforces a hard maximum of five rounds.
- **Java/Drools**: deterministic business rules such as “if P1 and SLA elapsed, create an escalation task.” This demo does not perform any state-changing actions.

The terms overlap in industry usage; the important architectural distinction is who chooses the next step and whether that choice depends on previous observations.

## 8. Production checklist

- Replace simulated methods with real backend service calls.
- Enforce authentication and authorization in Java services; never rely on the prompt as a security boundary.
- Validate customer/ticket ownership before querying data.
- Add timeouts, retries, rate limits, audit logs, and observability.
- Limit tool access to the minimum required; keep read-only tools separate from tools that change state.
- Require explicit authorization/confirmation for refunds, resets, escalations, and other consequential actions.
- Do not expose secrets, credentials, payment-card data, or sensitive logs to the model.
- Verify all outcomes against backend responses; never let the LLM claim an action occurred without confirmation.
