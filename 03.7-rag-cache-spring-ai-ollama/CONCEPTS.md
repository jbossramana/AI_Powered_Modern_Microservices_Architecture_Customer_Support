# Concepts: RAG with Caching

## 1. RAG

Retrieval-Augmented Generation has three main steps:
1. Retrieve relevant information from an external knowledge source.
2. Augment the prompt with the retrieved context.
3. Generate an answer using the LLM.

## 2. Indexing path and query path

Indexing: source documents → parsing/cleaning → chunks → embeddings → vector store.

Query path: user question → optional query transformation → search → optional reranking/compression → context added to prompt → LLM answer.

The sample indexes three refund/support policy documents when the app starts.

## 3. Embeddings do not mean retraining

An embedding model is a trained ML model that converts text into vectors. Adding a new document usually creates a new vector and stores it in the vector store; it does not update the embedding model's weights. Standard RAG also does not normally update the LLM's weights.

## 4. What is cached in this project?

This sample caches the final response returned by `RagService.answer()` using Spring Cache and Caffeine.

### Cache miss
1. Spring checks `ragAnswers`.
2. No entry is found.
3. The method runs.
4. `QuestionAnswerAdvisor` retrieves relevant context.
5. Ollama generates the answer.
6. Spring stores the answer in Caffeine.

### Cache hit
1. Spring finds an unexpired entry.
2. The stored answer is returned.
3. The annotated method body is skipped, so that call does not repeat vector retrieval or LLM generation.

## 5. RAG cache types

### Embedding cache
Stores the vector generated for identical text. A robust key includes normalized text hash, embedding model/version, dimensions, and preprocessing version. Benefit: fewer embedding computations.

### Query-transformation cache
Stores an LLM-rewritten query. Keys may need the normalized query, relevant chat context, rewrite prompt/model version, and tenant scope. Conversational references need careful treatment.

### Retrieval-result cache
Stores retrieved chunk IDs/scores or bounded results. Keys should account for transformed query, tenant/permission scope, filters, index version, top-k, embedding model, and retrieval settings. Invalidate or version results when documents or permissions change.

### Reranking cache
Stores relevance scores for the exact query and candidate document IDs/versions. It is invalid if the query or candidate set changes.

### Exact answer cache
Stores the final response for an equivalent request. Keys may need question, tenant/user scope, prompt/model version, index version, locale, and generation settings. This sample uses a simple normalized question key because it is a single-tenant teaching demo.

### Semantic answer cache
Embeds a new query and finds a semantically similar cached query. It can save work, but similar wording can hide a different product, date, condition, or intent. Measure false cache hits and use only when the risk is acceptable.

### Provider prompt-prefix caching
Some model providers internally reuse repeated prompt prefixes. This is provider/model-dependent and differs from application-level answer caching.

## 6. Caffeine settings in the project

- Maximum entries: 1,000.
- Expiration: 10 minutes after write.
- Statistics enabled: hits, misses, hit rate, and evictions.
- Caffeine is local to the process; entries disappear on restart and are not shared between app instances.

For multi-instance deployments, consider a shared cache such as Redis.

## 7. Invalidation and freshness

- TTL expires entries after a time interval.
- Versioned keys can make old index/prompt/model entries unreachable.
- Document-update events can clear or invalidate affected answers.
- Permission changes must also invalidate or re-check cached data.
- Live operational data (payment status, order status) should not be cached without a freshness guarantee.

The demo clears all answer-cache entries after adding a document. Production applications may require more targeted invalidation.

## 8. Security

- Do not reuse private answers across users or tenants.
- Include authorization scope in cache keys when responses depend on permissions.
- Enforce permissions in retrieval, not only in the LLM prompt.
- Re-check authorization before returning cached sensitive content.
- Treat cached content as sensitive as the source documents.
- Protect against prompt injection inside retrieved documents.
- Add authentication and authorization before exposing document-ingestion endpoints.

## 9. Advanced RAG concepts

- Query transformation: rewrite a question for better retrieval.
- Multi-query retrieval: generate several search variants and merge results.
- HyDE: use a hypothetical passage to help retrieve real documents; the hypothetical passage is not evidence.
- Hybrid search: combine semantic vector search with keyword search.
- Reranking: reorder retrieved candidates by relevance.
- Parent-document retrieval: match small chunks but return a larger section.
- Query decomposition: split a complex question into sub-questions.
- Contextual compression: remove irrelevant material while preserving conditions and evidence.
- Corrective RAG: assess retrieval quality and retry when evidence is weak.
- Agentic RAG: an agent chooses tools and retrieval steps.
- Graph RAG: use entity relationships for multi-hop questions.

Caching can make safe, equivalent steps cheaper, but it does not replace these techniques.

## 10. Important parameter distinction

- Retrieval `topK`: how many document chunks to retrieve.
- LLM temperature/top-p: controls on text generation.
These are different settings.

## 11. Evaluation and monitoring

Measure:
- Retrieval relevance, recall@k, and precision@k.
- Answer correctness, groundedness, and citation quality.
- Latency percentiles and model token usage.
- Cache hit/miss rates, stale-hit rate, evictions, and memory.
- Errors and the number of LLM calls avoided.

A high cache-hit rate is not useful if cached answers are stale or incorrect.

## 12. Suggested lab

1. Start Ollama and pull the chat and embedding models.
2. Run the Spring Boot app.
3. Ask one question twice.
4. Check cache stats after both requests.
5. Add a document and ask a related question.
6. Observe that the answer cache is cleared.
7. Experiment with TTL and maximum size.
8. For a production extension, add persistent vector storage, Redis, authorization-aware cache keys, versioned invalidation, and RAG evaluation.
