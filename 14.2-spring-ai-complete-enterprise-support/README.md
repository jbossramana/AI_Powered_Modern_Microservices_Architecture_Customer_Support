# Complete Spring AI Customer-Support Example

**Stack:** Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.1 · Ollama · Maven  
**Storage:** in-memory vector store, chat memory, approval state, and audit events  
**Docker:** not required  
**External database:** not required

This is the expanded example: it includes the original customer-support/order investigation flow plus the advanced concepts requested.

## 1. Prerequisites and model setup

Install JDK 21, Maven 3.9+, and Ollama. Ensure Ollama is running.

```bash
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

`qwen2.5:7b` generates responses. `nomic-embed-text` embeds documents and questions for semantic retrieval.

## 2. Start

Extract the ZIP and run from the project root:

```bash
mvn clean test
mvn spring-boot:run
```

The first build requires internet access to download Maven dependencies. The app defaults to Ollama at `http://localhost:11434`. Override with `OLLAMA_BASE_URL` if needed.

## 3. Demo credentials

- Support agent: `support-agent` / `change-me-agent`
- Approver: `approver` / `change-me-approver`

These are local-learning credentials. Change them before exposing the service to another user.

## 4. Endpoints

### A. RAG + memory + structured response + read-only tools

```bash
curl.exe -u "support-agent:change-me-agent" -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"ticketId\":\"TCK-100\",\"customerMessage\":\"My shipment is delayed. What should I do?\",\"orderId\":\"ORD-101\",\"conversationId\":\"case-100\"}" http://localhost:8080/api/support/chat
```

What happens:
1. Validate the incoming request.
2. Check order/payment records through read-only demo services.
3. Retrieve relevant knowledge-base documents from the in-memory vector store.
4. Send question, verified facts, retrieved passages, and conversation memory to Ollama.
5. Convert the answer into a typed `SupportReply`.
6. Write a metadata-only audit event.

### B. Streaming response

```bash
curl -N -u support-agent:change-me-agent "http://localhost:8080/api/support/stream?question=How%20do%20I%20handle%20a%20delayed%20shipment&conversationId=case-stream"
```
This endpoint streams model text as server-sent events. The project includes Spring MVC and WebFlux dependencies because Spring AI's streaming client uses Reactor types.

### C. Model-controlled tool calling

```bash
curl.exe -u "support-agent:change-me-agent" -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"ticketId\":\"TCK-100\",\"customerMessage\":\"Check ticket TCK-100 and order ORD-101, then explain the status.\",\"conversationId\":\"case-100\"}" http://localhost:8080/api/support/tool-chat
```

Only read-only tools are exposed to the model: ticket, order, payment, inventory, warranty, and SLA lookups. No write operation is exposed.

### D. Ticket classification, sentiment, priority, summary, draft response

```bash
curl -u support-agent:change-me-agent http://localhost:8080/api/support/tickets/analyze   -H 'Content-Type: application/json'   -d '{"ticketId":"TCK-200","customerMessage":"I think I was charged twice. Please help."}'
```

The output is structured and passed through `ResponseVerifier`. High-risk payment/security cases require human review. Suggested responses are drafts; they are not sent.

### E. Deterministic workflow orchestration

```bash
curl.exe -u "support-agent:change-me-agent" -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"ticketId\":\"TCK-100\",\"orderId\":\"ORD-101\",\"sku\":\"SKU-101\"}" http://localhost:8080/api/support/tickets/investigate
```

Java controls the sequence: ticket -> order -> payment -> inventory -> recommendation. This is appropriate when enterprise policy requires predictable, auditable steps.

### F. Additional agent patterns

No-tool answer: `POST /api/support/patterns/no-tool` with a `TicketRequest` JSON body.

Plan–execute–synthesize:
```bash
curl -u support-agent:change-me-agent http://localhost:8080/api/support/patterns/plan-execute-synthesize \\
  -H 'Content-Type: application/json' \\
  -d '{"question":"Investigate this delayed shipment","ticketId":"TCK-100","orderId":"ORD-101","sku":"SKU-101"}'
```
The model proposes a plan for display, but Java executes only the fixed allow-listed workflow. Model text is never executed as code.

Reflective self-review: `POST /api/support/patterns/reflect` with `{"question":"Draft a response to a customer asking for a refund."}`. One model call drafts an answer; a second reviews it for unsupported claims and unsafe promises.

Multi-agent-style routing: `POST /api/support/patterns/multi-agent-route` with `{"question":"I was charged twice","orderId":"ORD-202"}`. Java routes to a payment or order specialist prompt. These are role-specialized calls to one model, not separately deployed autonomous agents.

### G. Human approval workflow

Create a pending approval:

```bash
curl -u support-agent:change-me-agent -X POST http://localhost:8080/api/support/approvals   -H 'Content-Type: application/json'   -d '{"ticketId":"TCK-200","action":"REFUND_REVIEW","reason":"Customer reports duplicate charge"}'
```

Use the returned `requestId` to approve or reject:

```bash
curl.exe -u "approver:change-me-approver" -X POST -H "Content-Type: application/json" -H "Accept: application/json" -d "{\"decision\":\"REJECT\",\"note\":\"Needs payment reconciliation first\"}" http://localhost:8080/api/support/approvals/REQUEST_ID/decision

```

This example records a decision but deliberately does **not** issue refunds, send customer messages, or perform external side effects.

### H. Audit and observability

- Audit events: `GET http://localhost:8080/api/support/audit` (approver role)\n- Distributed tracing: Micrometer tracing + OTLP exporter configuration is included; configure an OTLP collector and `OTEL_EXPORTER_OTLP_ENDPOINT` to receive traces.
- Health: `GET http://localhost:8080/actuator/health`
- Metrics: `GET http://localhost:8080/actuator/metrics` (authenticated)

Logs include Spring AI advisor debugging. Actuator metrics expose application and JVM measurements. Add an OTLP collector and Micrometer tracing bridge if distributed traces are needed in your environment.

## 5. Implemented concepts

| Concept | Location |
|---|---|
| ChatClient and system/user prompts | `SupportAiService`, `AiConfiguration` |\n| Streaming | `GET /stream` |
| Ollama chat and embeddings | `application.yml`, Spring AI auto-configuration |
| RAG and vector similarity search | `RagService`, `KnowledgeBaseConfiguration` |
| Chat memory and advisor | `AiConfiguration`, `SupportAiService` |
| Tool calling | `SupportTools`, `/tool-chat` |
| No-tool, plan/execute/synthesize, reflection, specialist routing | `AgentPatternsService`, `/patterns/*` |
| Deterministic orchestration | `SupportWorkflowService`, `/tickets/investigate` |
| Ticket classification and structured output | `/tickets/analyze`, `TicketAnalysis` |
| Response verification | `ResponseVerifier` |
| Prompt validation | `PromptSafetyService` |
| Human approval | `ApprovalService`, `/approvals` |
| Authentication and authorization | `SecurityConfiguration`, method security |
| Retry/timeout/circuit-breaker teaching example | `ResilienceExecutor` |
| Audit | `AuditService` |
| Metrics/health | Actuator and `RequestMetrics` |
| Unit-test example | `ResponseVerifierTest` |

## 6. Important design notes

- **In-memory means volatile:** RAG documents, chat memory, audit events, and approval requests are lost on restart.
- **Timeout:** `ResilienceExecutor` bounds how long the caller waits. Java interruption does not guarantee cancellation of the underlying synchronous HTTP request. For strict network deadlines, configure connect/read timeouts on the HTTP client used by the selected Spring AI version and test the behavior.
- **Retry:** only safe because this demo calls a model and read-only lookups. Do not automatically retry writes or payments without idempotency controls.
- **Circuit breaker:** this compact implementation demonstrates the concept; use a mature resilience library and test concurrency/half-open behavior in production.
- **RAG:** the startup seed is intentionally small. Production RAG needs document loaders, chunking, document ACLs, versioning, retrieval evaluation, and source-grounding checks.
- **Prompt injection:** the lightweight validation is not a complete defense. Keep system instructions, constrain tools, validate authorization in Java, and treat retrieved text as untrusted.
- **Security:** HTTP Basic and hard-coded users are demo-only. Use OIDC/OAuth2, TLS, secrets management, rate limits, and least privilege in production.
- **Audit:** audit events avoid storing full customer prompts and model output. In-memory audit is neither durable nor tamper-proof.
- **Human approval:** this is a stateful in-memory demo, not a durable workflow engine. Production needs a database, idempotency, reviewer separation, and a separately authorized action executor.
- **MCP:** not connected to an external MCP server in this ZIP. The `SupportTools` methods demonstrate the local tool contract; MCP can be added when you have an MCP server and its access policy.
- **Streaming:** the core endpoints are synchronous for a simpler dependency surface. Streaming can be added using Spring AI's reactive `ChatClient.stream()` and an appropriate reactive web stack.
- **Build status:** the project is written against Spring Boot 4.1.0 and Spring AI 2.0.1 APIs, but this generation environment cannot run Maven. Execute `mvn clean test` locally to confirm dependency resolution and compile against Maven Central.

## 7. Architecture

```text
REST API + Spring Security
   |
   +-- /chat -----------------> validate -> deterministic read-only lookups
   |                                     -> RAG retrieval
   |                                     -> ChatClient + ChatMemory
   |                                     -> typed SupportReply
   |
   +-- /tool-chat ------------> ChatClient -> model selects read-only tools
   |
   +-- /tickets/analyze ------> structured classification -> response verifier
   |
   +-- /tickets/investigate --> Java-controlled fixed workflow
   |
   +-- /approvals ------------> pending state -> approver decision
   |
   +-- AuditService + Actuator metrics
```

## 8. Recommended learning sequence

1. Run `/tickets/investigate` to understand deterministic orchestration.
2. Run `/tool-chat` to see model-controlled tool selection.
3. Run `/chat` and inspect the retrieved sources to understand RAG.
4. Repeat a conversation using the same `conversationId` to see chat memory.
5. Run `/tickets/analyze` to inspect structured output and verification.
6. Run `/approvals` with support credentials, then decide with approver credentials.
7. Review `ResilienceExecutor`, `AuditService`, and Actuator metrics.
