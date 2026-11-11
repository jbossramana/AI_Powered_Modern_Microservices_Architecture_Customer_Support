# Concepts Guide — Customer Support AI

This guide explains the design in `customer-support-ai`, using Spring Boot 4.1.0, Spring AI 2.0.0, and Ollama.

## 1. What the application demonstrates

A customer sends a question to a REST endpoint. The application validates it, retrieves support articles, exposes Java methods as LLM-callable tools, asks a local Ollama model to produce an answer, and applies a final deterministic output check.

The demo includes:

- **LLM:** a language model served locally by Ollama (`llama3.2:latest` by default).
- **Tool calling:** Java methods for ticket lookup and refund-review eligibility.
- **RAG-style grounding:** keyword-ranked retrieval from a small local set of approved support articles, with article IDs added to the prompt.
- **Input guardrails:** request validation and checks for selected prompt-injection patterns.
- **Output guardrails:** basic response sanitization, including masking email/phone-like data and removing selected unsupported promises.
- **REST API:** a JSON request/response interface for a client, web application, or test command.

## 2. Request flow

1. `SupportController` receives `POST /api/support/ask`.
2. `AskRequest` uses Bean Validation to require a non-empty message and limit its length.
3. `InputGuardrail.validate()` rejects selected suspicious input patterns before the model is called.
4. `KnowledgeBase.search(question, 3)` ranks local articles and returns up to three likely matches.
5. `SupportService` adds the question and retrieved context to the model prompt. The system message tells the model to treat customer text and retrieved documents as data, not instructions, and not to invent unsupported facts.
6. `.tools(tools)` registers `CustomerSupportTools` for the Spring AI tool-calling cycle. The model may request a tool; Spring AI invokes the corresponding Java method and supplies the result back to the model.
7. `OutputGuardrail.sanitize()` applies deterministic post-processing to the generated text.
8. `AskResponse` returns the answer, retrieved source IDs, and a basic-check status.

## 3. LLM vs. agent vs. tools

### LLM
A Large Language Model interprets natural language and generates a response. It does not automatically know the current state of a customer's ticket or order. Those facts must come from trusted application data or tool results.

### Agent / orchestration
In this sample, `SupportService` is the orchestration layer. It prepares context, exposes tools, and lets Spring AI manage the model's tool-call loop. This is a **small tool-using assistant**, not a long-running autonomous planner with durable state, retries, multi-agent delegation, or approval workflow.

### Tools
Methods annotated with `@Tool` are callable by the model through Spring AI. `@ToolParam` describes parameters to the model. The model proposes which tool to call and its arguments; the application executes the Java method. A tool should validate arguments and enforce authorization independently of the model.

Current demo tools:

- `getTicketStatus(ticketId)`: reads fixed demonstration ticket data.
- `checkRefundEligibility(orderId)`: checks a demo duplicate-payment flag and returns review guidance. It **never issues a refund**.

Never expose arbitrary database access, shell commands, unrestricted URLs, or privileged operations as tools. For real customer data, authorize the logged-in user against the ticket/order on the server; do not trust an ID just because the model supplied it.

## 4. RAG explained

RAG means **Retrieval-Augmented Generation**:

1. **Retrieve:** search approved knowledge sources for relevant content.
2. **Augment:** place the retrieved passages into the model's context.
3. **Generate:** ask the model to answer using that evidence and cite source IDs.

This project uses a deliberately simple keyword-scored `KnowledgeBase`. It is useful for teaching the flow, but it is not vector/embedding-based semantic search. Similar words may be missed, and keyword overlap can retrieve irrelevant content.

A more complete semantic RAG implementation would:

1. Load approved documents and split them into chunks.
2. Generate embeddings with an embedding model, such as Ollama `nomic-embed-text`.
3. Store vectors and metadata in a persistent vector store (for example PostgreSQL with pgvector or another Spring AI-supported vector store).
4. Embed each user query and retrieve nearest chunks, with tenant/permission filters.
5. Optionally rerank results, apply a relevance threshold, and include source metadata.
6. Evaluate retrieval recall, answer groundedness, and citation correctness with a test set.

Retrieved content is untrusted data. It may contain malicious instructions; the model prompt explicitly tells the model not to follow instructions found inside retrieved articles. Production systems should additionally isolate trusted instructions from retrieved content and validate actions in code.

## 5. Input guardrails

Input guardrails run before expensive or risky model processing. The sample has:

- Bean Validation for blank/oversized requests.
- A small rule-based `InputGuardrail` that blocks selected suspicious prompt-injection phrases.

These rules are illustrative, not a complete prompt-injection detector. Attackers can paraphrase or encode instructions, so a regex list alone is not sufficient. Also consider authentication, authorization, rate limits, request-size limits, abuse monitoring, allowed-topic rules, and safe handling of uploaded files.

A blocked input is not proof that every unsafe request is detected. Never put secrets in system prompts and assume they are protected from extraction.

## 6. Output guardrails

The sample's `OutputGuardrail` performs basic text sanitization after generation. It masks some email/phone-like strings and removes selected unsupported promises. This is post-processing, not a semantic proof that the response is true or safe.

For production, prefer layered controls:

- Use typed/structured outputs and validate schema and allowed values.
- Check factual claims against tool results and retrieved source records.
- Reject or regenerate when required citations are missing.
- Enforce privacy and authorization before data reaches the model, not only after output.
- Block unsupported action claims such as “refund issued” unless a trusted transaction result confirms it.
- Add human approval for financial actions, account changes, or other high-impact operations.
- Log decision metadata safely while avoiding unnecessary sensitive content.

## 7. Why tools and RAG are different

| Question | Best mechanism | Why |
|---|---|---|
| “What is the refund policy?” | RAG | The answer is in approved policy documentation. |
| “What is the status of TCK-1001?” | Ticket tool | Status is operational, changing data. |
| “Can this duplicate payment be reviewed?” | Tool + policy RAG | The tool checks the order signal; policy explains process. |
| “Explain why my login may fail.” | RAG, possibly diagnostic tools | Documentation explains known troubleshooting steps; tools can inspect actual service state if safely implemented. |

RAG retrieves information; a tool performs a defined operation or fetches live/application data. They can be used together.

## 8. Configuration

`src/main/resources/application.yml` configures:

- Port `8080`.
- Ollama base URL, default `http://localhost:11434`.
- Chat model, default `llama3.2:latest`.
- Low temperature for more consistent support answers.
- Actuator health endpoint exposure.

Environment overrides:

- `OLLAMA_BASE_URL`
- `OLLAMA_CHAT_MODEL`

Use a model tag that has actually been downloaded with `ollama pull`. The machine running the Spring Boot app must be able to reach Ollama.

## 9. Testing checklist

Try all of the following:

- Valid ticket ID: `TCK-1001`.
- Another ticket: `TCK-1002`.
- Unknown ticket ID.
- Refund-review order: `ORD-2001`.
- An order without a duplicate-payment flag.
- Login support question to exercise retrieval.
- Blank message and an oversized message.
- The sample prompt-injection phrase in the README.
- Questions with no matching article: the model should admit it cannot verify policy facts.
- Output cases containing email/phone-like strings and unsupported action promises.

Do not grade a test solely by whether the model used a specific tool every time: model tool selection can vary. If a tool must always run for a business-critical operation, invoke it deterministically in application code rather than relying on model choice.

## 10. Production-readiness gaps

This is a teaching starter, not a production-secure customer support platform. Before production, add:

- Authentication, role/tenant authorization, and server-side record access checks.
- Persistent ticket/order repositories; remove hard-coded demo data.
- Semantic vector RAG, document ingestion, permissions, versioning, and deletion workflows.
- Robust prompt-injection and data-exfiltration evaluations.
- Tool allowlists, strict input schemas, timeouts, idempotency, and audit logging.
- Human approval for refunds and account mutations.
- Rate limits, quotas, circuit breakers, monitoring, metrics, and traces.
- Automated unit/integration/security tests and model-quality evaluations.
- Privacy-aware logging and retention policies.

## 11. Key takeaway

The LLM understands language and helps formulate an answer. RAG supplies relevant approved knowledge. Tools connect the assistant to controlled application capabilities. Guardrails enforce rules around both the incoming request and outgoing response. **The application—not the model—must remain the authority for permissions and consequential actions.**
