# Module 5 — Retrieval-Augmented Generation (RAG)
## Concepts Explained in the Spring AI + Ollama Code

This note explains the concepts demonstrated by the `03-rag-spring-ai` project. It distinguishes features that are implemented in the code from advanced production concepts that are useful next steps.

## 1. What is RAG?

Retrieval-Augmented Generation combines two operations:

1. **Retrieval:** find relevant passages from a trusted knowledge base for the user's question.
2. **Generation:** give those passages to a language model (LLM) and ask it to produce an answer grounded in that evidence.

Without RAG, an LLM mainly relies on information learned during training and the current prompt. That information may be outdated, incomplete, or unrelated to an organization's private policies. RAG lets an application supply current, domain-specific information at question time.

**Example:** A customer asks, “I was charged twice this month. Can I get a refund?” The application retrieves the billing/refund article and asks the LLM to answer using that article, rather than inventing a refund policy.

RAG is not model fine-tuning. It does not permanently teach the model new information; it retrieves information at runtime and places it in the prompt.

## 2. High-level request flow

The implemented flow is:

1. Knowledge-base Markdown files are loaded at application startup.
2. Each article is split into smaller document chunks.
3. An embedding model converts each chunk into a numerical vector.
4. The vectors and their documents are stored in `SimpleVectorStore`.
5. A client calls `POST /api/support/ask` with a question.
6. `RetrievalAugmentationAdvisor` searches for relevant chunks using vector similarity.
7. The retrieved chunks are inserted into the model prompt as context.
8. Ollama's chat model generates a response under the configured system instructions.
9. The API returns the answer and performs a separate diagnostic search to return source excerpts and metadata.

The diagnostic search used to populate the response's `sources` is a second similarity search. It is intended for explanation and debugging; it is not guaranteed to be an exact trace of the context that the advisor supplied to the model.

## 3. Why enterprise AI needs RAG

Enterprise answers often depend on internal or frequently changing material, such as:

- Support procedures and troubleshooting steps
- Product features, plans, quotas, and pricing
- Billing, refund, and account policies
- Security and access-control documentation
- HR, compliance, and operational policies

RAG can improve factual grounding and make answers easier to audit because source documents can be surfaced. It does **not** guarantee correctness: retrieval may miss the right passage, documents may be stale, and a model may still misinterpret the context. Critical decisions require validation, access control, and often human review.

## 4. Knowledge base and source documents

The project stores Markdown articles under:

`src/main/resources/knowledge-base/`

Examples include billing and refunds, password reset and two-factor authentication, storage limits, data export/account deletion, and shared-folder permissions.

`RagChatConfig.ingestKnowledgeBase(...)` finds files using `classpath:knowledge-base/*.md`. `TextReader` reads each resource into Spring AI `Document` objects. The code adds the source filename to custom metadata so that retrieved content can be associated with an article.

A Spring AI `Document` contains text plus metadata. Metadata can carry a source filename, document identifier, department, tenant, version, URL, or access classification. The demo records the filename; production systems should define and validate a consistent metadata schema.

### Ingestion timing

The demo uses `ApplicationRunner`, so ingestion happens when the application starts. This is simple for a small, static sample corpus. It also means the in-memory index is rebuilt at startup and changes to files generally require a restart/re-ingestion.

A production ingestion pipeline is usually event-driven or scheduled, supports incremental updates and deletions, records document versions, and reports failed ingestion jobs.

## 5. Chunking and overlap

Embedding an entire long document as one unit can make retrieval too broad. Chunking splits a document into smaller passages so that retrieval can return the portion relevant to a question.

The code uses Spring AI's `TokenTextSplitter`:

```java
TokenTextSplitter splitter = new TokenTextSplitter();
List<Document> chunks = splitter.apply(reader.get());
```

Each chunk is then added to the vector store. Smaller chunks can improve precision but may lose context; larger chunks preserve more context but can contain unrelated material and use more prompt tokens. Overlap can preserve facts that cross chunk boundaries. The demo uses the splitter's defaults rather than explicitly configuring a chunk size and overlap.

For production, evaluate chunk size, overlap, headings, tables, code blocks, and document structure against real user questions. Preserve source identifiers and useful metadata on every chunk.

## 6. Embeddings

An embedding is a numeric vector representing semantic features of text. Texts with related meanings often have vectors that are close under a similarity measure, even if they use different words.

The application configures Ollama's embedding model in `application.yml`:

```yaml
spring:
  ai:
    ollama:
      embedding:
        options:
          model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
```

Spring AI injects an `EmbeddingModel` into the vector-store bean. When documents are added, the vector store uses the embedding model to create vectors; when a query is searched, the query is embedded in the compatible vector space.

**Important:** document embeddings and query embeddings must use a compatible model and vector dimensions. If the embedding model changes, re-embed the corpus rather than mixing vectors from incompatible models.

## 7. Vector stores and vector similarity

A vector store keeps document content, metadata, and embeddings, and supports similarity searches. In this project:

```java
return SimpleVectorStore.builder(embeddingModel).build();
```

`SimpleVectorStore` is an in-memory implementation useful for learning and small demos. It is not durable storage; the index is lost when the process stops.

The retriever uses a similarity threshold and Top-K setting to decide which documents to return. Similarity scores are implementation/model-dependent and should be calibrated with representative test data; a threshold such as `0.45` is a demo setting, not a universal quality standard.

Other vector-store choices for larger or persistent deployments include PostgreSQL with pgvector, Redis, Elasticsearch/OpenSearch, and managed vector databases. Choose based on persistence, filtering, scale, operations, security, and cost requirements.

## 8. Top-K retrieval

Top-K means retrieving at most K of the highest-ranked matching chunks. The advisor is configured with:

```java
VectorStoreDocumentRetriever.builder()
    .vectorStore(vectorStore)
    .similarityThreshold(0.45)
    .topK(4)
    .build()
```

Here, the retriever is configured to return up to four chunks that satisfy the threshold. A high K may supply more evidence but can add noise and consume the model's context window. A low K can omit needed details. Tune K using evaluation questions and measure whether the relevant evidence appears in the retrieved set.

The diagnostic endpoint separately uses `SearchRequest` with the same Top-K and threshold values. Keep these settings aligned if you want diagnostics to resemble the advisor's retrieval configuration.

## 9. Similarity threshold

The threshold filters out results whose similarity is considered too low. It can help avoid supplying unrelated material, but it creates a trade-off:

- Too low: irrelevant passages may be included.
- Too high: useful passages may be excluded, making the model unable to answer.

Do not assume a threshold has the same meaning across embedding models, vector stores, or similarity metrics. Validate it against known-answer and unanswerable-question test sets.

## 10. Grounded answer generation and ChatClient

The application builds a Spring AI `ChatClient` and sets a default system prompt. The system prompt tells the assistant to use retrieved knowledge as the source of truth, avoid inventing policies or prices, admit when information is insufficient, and recommend human support when appropriate.

The `ChatClient` is configured with `RetrievalAugmentationAdvisor`. On a request, the advisor retrieves context and adds it to the prompt before the chat model is called. The controller can therefore call:

```java
String answer = ragChatClient.prompt()
    .user(request.question())
    .call()
    .content();
```

The controller does not manually build the retrieval prompt; the advisor handles that integration. This is one of the key Spring AI concepts shown by the project: advisors can add reusable behavior around a chat interaction.

Grounding reduces unsupported claims but cannot guarantee that every generated statement is supported. For higher-risk use cases, add explicit citation-to-claim checks, deterministic policy logic, evaluation, and human approval where appropriate.

## 11. Handling questions outside the knowledge base

The system prompt instructs the model to say when the retrieved context does not answer the question and to direct the user to human support. The retrieval threshold can also prevent weak matches from being passed as context.

This is a **prompt-based fallback**, not a formal guarantee that an answer is unanswerable. The model may still guess, and the diagnostic `sources` list is not itself a definitive test of what the advisor retrieved.

A stronger implementation can:

1. Check whether retrieval returned enough relevant evidence.
2. Apply a calibrated relevance/coverage threshold.
3. Return a structured `INSUFFICIENT_CONTEXT` result when evidence is inadequate.
4. Ask a clarifying question when the user's request is ambiguous.
5. Route sensitive or unresolved requests to a human agent.
6. Log the reason for fallback without leaking private customer data.

## 12. Prompt-injection awareness

The system prompt says that user messages and retrieved documents are untrusted data and that instructions inside retrieved documents must not override the assistant's role. This is a useful defensive instruction because retrieved content could contain malicious or irrelevant instructions.

Prompt instructions alone are not a security boundary. Also restrict ingestion sources, sanitize or review documents, keep secrets out of model context, enforce authorization in application code, and test attacks in which a document asks the model to reveal data or ignore policy.

## 13. REST API and request validation

The controller is a Spring MVC `@RestController` rooted at `/api/support`.

### `POST /api/support/ask`

Accepts JSON such as:

```json
{
  "question": "I was charged twice this month. Can I get a refund?"
}
```

Returns an `AskResponse` with:

- `answer`: generated response
- `sources`: diagnostic source names, excerpts, and metadata
- `groundingNote`: a note explaining whether the diagnostic search found matching chunks and reminding callers to verify the demo output

### `POST /api/support/retrieve`

Accepts the same request body but does not generate an LLM answer. It returns the query, result count, and matching source chunks. This separates retrieval debugging from generation debugging.

### Validation

`AskRequest` uses `@NotBlank` and `@Size(max = 4000)`, and the endpoints use `@Valid`. This rejects blank questions and questions longer than 4,000 characters when Bean Validation is correctly configured. Production APIs should also set request timeouts, rate limits, authentication, authorization, and consistent error responses.

## 14. Source metadata and diagnostics

`retrieveSources(...)` uses `VectorStore.similaritySearch(SearchRequest)` to run a diagnostic search. `toSource(...)` returns the source name, up to 500 characters of the chunk text, and metadata. This helps learners see what retrieval found and troubleshoot poor answers.

These source excerpts are not guaranteed citations for every sentence in the generated answer. In addition, exposing raw metadata or excerpts can leak internal content. A production API should filter fields, apply document-level authorization before retrieval, and return only sources the current user is allowed to access.

## 15. Ollama configuration

The application connects to Ollama at `http://localhost:11434` by default. Configuration can be overridden with environment variables:

- `OLLAMA_BASE_URL`: Ollama server URL
- `OLLAMA_CHAT_MODEL`: chat model name; default `llama3.2:latest`
- `OLLAMA_EMBEDDING_MODEL`: embedding model name; default `nomic-embed-text`

The chat temperature is set to `0.2`, encouraging less varied responses, though it does not guarantee factuality. The configuration uses `pull-model-strategy: when_missing` so the Ollama integration can pull configured models when they are missing, depending on the Spring AI/Ollama version and environment.

The chat model and embedding model serve different purposes: the chat model writes the answer; the embedding model creates vectors for retrieval.

## 16. Important limitations of the current implementation

The following are implemented in the sample:

- Startup ingestion of local Markdown files
- Token-based splitting using default splitter settings
- Ollama embeddings
- In-memory `SimpleVectorStore`
- Top-4 vector retrieval and a 0.45 similarity threshold
- `RetrievalAugmentationAdvisor`-based context injection
- Prompt-based instruction to avoid unsupported answers
- Ask and retrieval-diagnostics endpoints
- Basic request validation and source excerpts

The following are **not implemented** merely by this sample and should be treated as future enhancements:

- Persistent vector storage and incremental ingestion
- Hybrid keyword + vector retrieval
- Cross-encoder or LLM-based reranking
- Query rewriting, decomposition, or multi-query retrieval
- Document-level authorization and tenant isolation
- Formal citation verification
- Automated retrieval/answer evaluation and quality dashboards
- Structured confidence or abstention decisions
- Conversation memory or multi-turn question resolution
- Production metrics, tracing, rate limiting, and authentication
- Human handoff workflow

## 17. Advanced RAG concepts to learn next

### Hybrid search
Combines semantic vector search with lexical search such as BM25. Vector search handles meaning and paraphrases; keyword search can be better for exact product names, error codes, IDs, and policy terms. The results need to be merged and ranked. **Not implemented in this project.**

### Reranking
A second-stage model scores query-document pairs and reorders the initially retrieved candidates. A common design retrieves a wider candidate set, then keeps the best few. This can improve relevance at added latency and compute cost. **Not implemented.**

### Query rewriting and multi-query retrieval
Rewrite conversational or ambiguous questions into a search-friendly query, or generate multiple related queries and combine results. This can improve recall but may increase latency and introduce query drift. **Not implemented.**

### Metadata filtering and access control
Filter by product, language, version, tenant, or permission before returning documents. Authorization must be enforced by trusted application logic, not only by asking the LLM to respect permissions. **The demo records source metadata but does not implement authorization filters.**

### Context compression
Remove irrelevant sentences or summarize retrieved chunks before sending them to the model. This can reduce token usage but risks dropping critical details. **Not implemented.**

### Retrieval evaluation
Measure whether the correct evidence is retrieved using metrics such as Recall@K, Precision@K, and Mean Reciprocal Rank (MRR). Evaluate generated answers separately for correctness, faithfulness to sources, relevance, and appropriate abstention. Use a fixed benchmark of realistic answerable and unanswerable questions. **Not implemented.**

### Answer evaluation and faithfulness
Check whether claims in the final answer are supported by retrieved passages. Human-reviewed examples are important because automated judges can also be wrong. **Not implemented.**

### Query routing
Send different question types to different corpora or tools—for example, billing questions to billing documents and technical errors to troubleshooting documents. **Not implemented.**

### Persistent indexing and document lifecycle
Use a persistent vector store, stable document IDs, version tracking, re-embedding when content or embedding models change, and deletion handling. **Not implemented.**

### Observability
Track latency by stage, retrieval result counts, model errors, token usage where available, fallback rate, and quality scores. Avoid logging sensitive prompt content by default. **Not implemented.**

## 18. Production checklist

Before using a RAG service for real customer support:

- Replace the in-memory store with persistent storage appropriate for the workload.
- Build an ingestion process with update, delete, version, and retry support.
- Tune chunking, Top-K, and similarity thresholds using a representative evaluation set.
- Add authentication, authorization, tenant isolation, and document-level filters.
- Do not expose private source text or metadata to unauthorized callers.
- Add structured abstention and human escalation for unsupported or high-impact questions.
- Test prompt injection, data leakage, malformed input, and adversarial questions.
- Add timeouts, rate limiting, error handling, metrics, tracing, and operational alerts.
- Track source freshness and ensure policy documents have owners and review dates.
- Validate compatibility of the selected Spring Boot, Spring AI, Java, and model versions in the actual build environment.

## 19. Suggested hands-on experiments

1. Ask a question clearly answered by one knowledge-base article and inspect the source chunks.
2. Ask a question that uses different wording from the article to observe semantic retrieval.
3. Ask for a policy not present in the knowledge base and observe whether the assistant abstains appropriately.
4. Compare changing Top-K from 4 to 2 or 6; observe relevance, context size, and answer quality.
5. Compare several similarity thresholds using the same test questions.
6. Add a new Markdown article and restart the application to see startup ingestion.
7. Change the embedding model only after planning a full re-embedding of the corpus.
8. Try a malicious instruction in a user question and in a test document; verify that prompt instructions are not treated as a substitute for security controls.

## 20. Summary

This project demonstrates the core RAG pipeline with Spring AI and Ollama: ingest documents, split them into chunks, embed and index them, retrieve the most similar chunks, inject them into the prompt with `RetrievalAugmentationAdvisor`, and generate a grounded support response. The `/ask` endpoint demonstrates the user-facing workflow, while `/retrieve` makes retrieval independently inspectable. The code is a learning example, not a complete production security or evaluation framework.
