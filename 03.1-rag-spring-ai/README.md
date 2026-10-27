# Step 3 — Retrieval-Augmented Generation (Spring AI)

Five knowledge-base articles get embedded into an in-memory vector store on startup. The ask API returns source excerpts, and a retrieval-only endpoint helps inspect the Top-K results.
Every question now gets answered from your actual documentation, with the model told
explicitly to say "I don't know" rather than invent a policy — instead of relying on
whatever the base model happened to memorize about generic SaaS billing policies.

## What this teaches

- Building a `VectorStore` from an `EmbeddingModel` (`SimpleVectorStore` here; swap for
  `PgVectorStore`/`RedisVectorStore`/etc. in production — the rest of the code doesn't change)
- Chunking documents with `TokenTextSplitter` and ingesting them with an `ApplicationRunner`
- `RetrievalAugmentationAdvisor` + `VectorStoreDocumentRetriever`: Spring AI's modular RAG
  advisor that retrieves and injects context automatically, before the model is ever called
- Why the system prompt matters as much as retrieval quality — "answer only from context"
  is what prevents the model from filling gaps with plausible-sounding guesses

## Prerequisites

```bash
ollama pull qwen3:8b
ollama pull nomic-embed-text
```

`nomic-embed-text` is a small, fast embedding-only model — this is a separate pull from
your chat model.

## Run it

```bash
cd 03-rag-spring-ai
mvn spring-boot:run
```

Watch the startup logs for `Ingested 5 knowledge-base article(s) into N chunk(s)` — that
confirms ingestion ran before the app started accepting requests.

## Try it

curl -s http://localhost:8080/api/support/ask -H "Content-Type: application/json" -d "{\"question\":\"I was charged twice this month, can I get a refund?\"}"

curl -s http://localhost:8080/api/support/ask -H "Content-Type: application/json" -d "{\"question\":\"Do you support Bitcoin payments?\"}" 

The first should cite the duplicate-charge policy from `billing-and-refunds.md`
(full refund, escalate to a human to verify). The second isn't covered by any article —
watch it say so instead of guessing, since the system prompt explicitly forbids that.

## Additional endpoints

- `POST /api/support/ask` — generates a grounded answer and returns likely matching source excerpts/metadata.
- `POST /api/support/retrieve` — returns Top-K retrieved chunks without calling the chat model, useful for debugging relevance.

Example (Windows PowerShell):
```powershell
curl.exe -X POST "http://localhost:8080/api/support/retrieve" -H "Content-Type: application/json" -d "{\\"question\\":\\"How do I reset my password?\\"}"
```

See [NOTES.md](NOTES.md) for advanced concepts: chunking tradeoffs, thresholds, metadata, query transformation, hybrid search, reranking, evaluation, prompt-injection risks, authorization, and production hardening.

## How it works

1. On startup, `ingestKnowledgeBase` reads every `.md` file in
   `resources/knowledge-base/`, splits it into chunks with `TokenTextSplitter`, embeds
   each chunk via the Ollama `EmbeddingModel`, and stores it in the `VectorStore` bean.
2. `ragChatClient` attaches a `RetrievalAugmentationAdvisor` to the `ChatClient` as a
   **default advisor** — it runs on every request, retrieves the top 4 chunks above a
   0.45 similarity threshold, and injects them into the prompt before the model sees the
   question.
3. `SupportRagController` is unaware any of this is happening — same two-line shape as
   every controller so far.

## Tuning retrieval

`similarityThreshold` (0.45 here) and `topK` (4) in `RagChatConfig` are the two knobs
you'll adjust most. Too high a threshold and legitimate questions get no context (the
model correctly says "I don't know" about things that *are* documented); too low and
irrelevant chunks get pulled in and can confuse the answer. If you add your own
knowledge-base files, re-tune these against real questions.

## What's next

`04-mcp-spring-ai` takes the tool-calling idea from Module 1 and exposes it over the
**Model Context Protocol** instead of local `@Component` beans — the same customer/ticket
tools, but reachable from any MCP-compatible client, not just this one Spring app.


## Version and validation
The project declares Java 21, Spring Boot 4.1.0, and Spring AI 2.0.0. Run `mvn clean test` locally to validate dependencies and compilation in your environment.
