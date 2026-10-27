# LangChain4j Concepts — Implementation Notes

## 1. What LangChain4j is

LangChain4j is a Java library ecosystem for connecting applications to chat models, embedding models, vector stores, tools and retrieval workflows. It provides low-level APIs and higher-level AI Services. Official documentation: https://docs.langchain4j.dev/intro/ and https://docs.langchain4j.dev/tutorials/ai-services/.

## 2. AI Services and declarative interfaces

`SupportAssistant`, `MemoryAssistant` and `TriageAssistant` are plain Java interfaces. `AiServices.builder(...).build()` creates implementations that translate method arguments and annotations into model requests. This separates the business-facing Java contract from the underlying model API. It is similar in spirit to Spring Data repository interfaces, but it is an AI interaction abstraction rather than a database repository.

## 3. Prompts and instructions

`@SystemMessage` expresses stable assistant behavior; `@UserMessage` identifies the current user input. Prompts guide a model but are not a security boundary. Never put secrets in prompts, and never trust model text as authorization to execute a privileged action. Enforce access control and business policy in Java services.

## 4. Provider abstraction and configuration

`AssistantConfig` conditionally constructs an Ollama or OpenAI `ChatModel`. `SUPPORT_PROVIDER=ollama` is the default; set `SUPPORT_PROVIDER=openai` and `OPENAI_API_KEY` to switch. The same AI Service interface can then be used with either model implementation. Providers still differ in supported context length, tool calling, structured output, latency, cost and model behavior, so switching is not always behavior-identical.

## 5. Tool/function calling

`SupportTools` registers methods with `@Tool`. The model receives tool names/descriptions and may request a call. LangChain4j dispatches that call to Java; the result is provided back to the model so it can formulate an answer. The model does not execute Java itself. Tools must be small, well-described, authenticated and authorized. Validate arguments, enforce tenant ownership, use timeouts, audit calls and require human approval for consequential actions. The sample ticket lookup is a hard-coded demo and does not connect to a ticketing system.

## 6. Chat memory

`MemoryAssistant` uses `@MemoryId` to select a conversation and a `ChatMemoryProvider` that creates a `MessageWindowChatMemory` with up to 20 messages. The same session ID reuses the same memory in this application process; a different ID isolates a different chat. In-memory memory is lost on restart and is not distributed across replicas. A production deployment needs a durable store, TTL/retention, tenant isolation, deletion controls and safeguards against accepting attacker-controlled session IDs as identity.

## 7. Typed/structured output

`TriageAssistant.triage()` returns a Java record (`TicketTriage`) rather than free-form text. AI Services can parse model output into Java types; providers/models may support stronger JSON-schema-constrained output when configured. This sample demonstrates typed results, but does not claim that schema-constrained generation is enabled for every provider. Always validate enum values, bounds and business invariants after parsing; never directly execute recommended actions from the model output.

## 8. Retrieval-Augmented Generation (RAG)

The `/api/support/ask` flow is:

1. Search the knowledge base for relevant snippets.
2. Add the selected snippets to a prompt with explicit grounding instructions.
3. Ask the model to answer only from those snippets.
4. Return both the answer and the retrieved sources.

The sample retriever is deliberately simple keyword-overlap retrieval in `KnowledgeService`, not vector search. It is deterministic, easy to inspect and has no external infrastructure dependency, but it misses synonyms and semantic matches. For production-grade semantic RAG, use an embedding model, chunking, an `EmbeddingStore`, metadata filters, top-K retrieval, and optionally reranking. LangChain4j has document loaders/parsers, text segmenters, embedding models, vector-store integrations and `EmbeddingStoreContentRetriever`/`EmbeddingStoreIngestor` APIs. See https://docs.langchain4j.dev/tutorials/rag/.

### Production RAG upgrades

- Split PDFs/HTML/Word files into appropriately sized chunks with overlap.
- Attach source, tenant, ACL, document version and timestamp metadata to every chunk.
- Embed chunks and persist vectors in a supported store (for example pgvector, Qdrant, Milvus or Elasticsearch integration).
- Filter by tenant and access rights before retrieval; never rely on the LLM to enforce document permissions.
- Evaluate retrieval recall and answer faithfulness with a curated test set.
- Return source references and abstain when evidence is weak.
- Treat retrieved text as untrusted; it can contain prompt injection.

## 9. Ingestion endpoint

`POST /api/knowledge` adds one text item to the in-memory knowledge store. It validates fields and rejects duplicate IDs. This is a teaching endpoint, not a document ingestion pipeline. It has no authentication, persistence, file parsing, virus scanning, ACL or chunking.

## 10. Prompt-injection and safety boundaries

The system prompt says user text and retrieved snippets are untrusted. That is useful defense-in-depth but cannot guarantee safety. The application must separately:

- validate inputs and cap sizes;
- authorize every data access and tool call;
- separate trusted instructions from untrusted content;
- avoid exposing secrets and internal prompts;
- restrict outbound network and filesystem access;
- use allowlists and deterministic policy checks;
- require approval for payments, account changes, deletion or other high-impact operations;
- log metadata without leaking sensitive prompts or PII.

LangChain4j also documents guardrails, but notes that its guardrails feature is experimental and API behavior can change: https://docs.langchain4j.dev/tutorials/guardrails/.

## 11. Streaming

LangChain4j supports streaming model APIs and token-by-token responses. This sample does not expose a streaming endpoint; a useful extension is a `StreamingChatModel` plus Spring MVC `SseEmitter` or WebFlux `Flux`, with disconnect handling, cancellation, timeouts and backpressure. Streaming and typed structured output have provider-specific limitations.

## 12. Multimodality

LangChain4j integrations support multimodal inputs for compatible models, including images and audio in supported provider modules. Support depends on model, provider and message content APIs. This sample is text-only. Add a dedicated endpoint and file validation before accepting image/audio uploads.

## 13. Agents and orchestration

An AI Service with tools can perform a tool-use loop, but that does not automatically make it a safe multi-agent workflow. LangChain4j documents agentic patterns and MCP-based tool agents: https://docs.langchain4j.dev/tutorials/agents/. For business workflows, define bounded steps, timeouts, retries, idempotency, budgets, trace IDs, and explicit human approval gates. Do not give autonomous agents unrestricted shell, SQL or production write access.

## 14. MCP

Model Context Protocol standardizes how an AI application discovers and invokes tools/resources exposed by MCP servers. LangChain4j has MCP integrations in separate modules. This project does not implement an MCP server or client; the Java `@Tool` examples are local in-process tools, not MCP. Add MCP when tool capabilities must be shared across applications or hosted independently.

## 15. Observability and evaluation

The app exposes Spring Boot Actuator health. That is not full LLM observability. Production observability should record request correlation IDs, model/provider, latency, token usage where available, tool names and outcomes, retrieval IDs/scores, errors and user feedback. Redact secrets and personal data. Build evaluation datasets for answer quality, retrieval recall, refusal behavior, tool correctness, latency and cost. LangChain4j documents AI Service observability, which is an evolving/experimental capability: https://docs.langchain4j.dev/tutorials/observability/.

## 16. Testing strategy

- Unit-test deterministic retrieval, validation and authorization without an LLM.
- Use a fake/stub model for controller tests.
- Keep a small integration test set against the actual provider.
- Test empty retrieval, irrelevant questions, malicious retrieved text, invalid tool arguments, timeouts and malformed model output.
- Evaluate semantic quality separately from unit-test pass/fail; LLM output is nondeterministic.

## 17. Current sample boundaries

Included: AI Services, system/user prompt annotations, local provider switching, tool calling, in-memory per-session memory, typed triage, simple RAG-style grounding, knowledge ingestion/listing, validation, health endpoint, documentation and tests.

Not implemented as production features: vector/embedding retrieval, persistent memory, document/PDF parsing, reranking, streaming API, image/audio, MCP, multi-agent orchestration, provider-side JSON Schema enforcement, auth/RBAC, durable audit, distributed tracing, rate limiting, human approval workflow. These are extension areas, not hidden capabilities of this ZIP.
