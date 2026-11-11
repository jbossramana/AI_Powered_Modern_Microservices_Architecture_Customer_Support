# RAG Customer Support — Concepts Explained

## 1. What is RAG?

Retrieval-Augmented Generation combines information retrieval with a language model.

1. Retrieve relevant information from a knowledge source.
2. Add the retrieved text to the model's context.
3. Ask the model to formulate an answer grounded in that context.

RAG can reduce unsupported answers, but it does not guarantee correctness.

## 2. Why use website content?

A support website may contain FAQs, product documentation, return policies, warranty terms, and shipping details. Indexing these pages lets customers ask natural-language questions instead of navigating pages manually.

This demo ingests only explicitly configured URLs. It does not recursively crawl an entire website.

## 3. Ingestion pipeline

### Fetch
Jsoup downloads the HTML for a configured URL.

### Extract
Navigation, scripts, styles, headers, and footers are removed in this example. The remaining body text is extracted. Real sites may require site-specific extraction.

### Chunk
Long text is divided into smaller documents. Chunking helps retrieval find the relevant section instead of supplying an entire website page.

### Embed
The `nomic-embed-text` model converts each chunk into a numeric vector representing semantic features of the text.

### Store
`SimpleVectorStore` stores the chunk text, metadata, and embedding in the application's memory.

## 4. Embeddings and semantic search

An embedding is a list of numbers representing text in a vector space. Texts with related meaning may have vectors that are close under a similarity measure.

At question time, the embedding model vectorizes the user's question. The vector store compares it with stored document vectors and returns the most relevant chunks.

This is semantic retrieval, not a keyword-only search. Results still depend on the embedding model, chunk quality, and the content that was indexed.

## 5. SimpleVectorStore

`SimpleVectorStore` is Spring AI's in-memory vector store.

Advantages:
- No database installation.
- Quick to set up for learning and prototypes.
- Uses the same `VectorStore` abstraction as other Spring AI stores.

Limitations:
- Data is normally lost when the process stops.
- Memory use grows with the number and size of chunks.
- Not suitable for a multi-instance production deployment without additional persistence and coordination.

For a demo, run ingestion again after restarting the application.

## 6. Ollama: chat model vs embedding model

### `llama3.2`
Generates the natural-language response from the user's question and retrieved context.

### `nomic-embed-text`
Creates embeddings for both indexed chunks and incoming questions.

These are separate tasks. The chat model does not replace the embedding model.

Ollama runs the models locally, but the first model download requires network access. Performance depends on machine memory, CPU/GPU, and model size.

## 7. Spring AI components

### `EmbeddingModel`
Spring AI abstraction used to turn text into embeddings. The Ollama starter configures an Ollama-backed implementation.

### `VectorStore`
Common API for adding documents and performing similarity searches.

### `SimpleVectorStore`
The in-memory implementation used in this project.

### `TokenTextSplitter`
Splits long documents into smaller chunks.

### `ChatClient`
Fluent Spring AI API for calling the chat model.

### `QuestionAnswerAdvisor`
Connects vector search to a chat request. It retrieves relevant context and makes it available to the model.

## 8. Runtime question flow

1. The client sends a question to `/api/support/chat`.
2. The controller validates the question.
3. `ChatClient` starts the request.
4. `QuestionAnswerAdvisor` searches the vector store.
5. The matching website chunks are included in the model context.
6. Ollama's `llama3.2` generates a response.
7. The API returns the answer as JSON.

## 9. Why index before asking questions?

The vector store is empty until documents are added. The `/api/support/ingest` endpoint loads configured pages and stores chunks. Run it after application startup and after every restart.

In a larger application, ingestion would typically run in a scheduled job or separate worker, with deduplication, update detection, retry logic, and monitoring.

## 10. Limitations and production safeguards

- Website text can be outdated; schedule re-indexing.
- HTML extraction can include irrelevant content or miss JavaScript-rendered content.
- Retrieved website content may contain prompt-injection text. Treat it as untrusted reference material.
- Restrict ingestion to trusted, allow-listed hosts; never accept arbitrary fetch URLs from anonymous users.
- Add authentication, rate limiting, logging, timeouts, and human escalation.
- Include source URLs in answers when traceability matters.
- Evaluate retrieval quality and answer quality with representative customer questions.
- Consider a persistent vector database for larger or multi-instance systems.
