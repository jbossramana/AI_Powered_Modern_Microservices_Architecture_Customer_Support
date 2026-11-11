# RAG + Answer Caching with Spring AI and Ollama

A demonstration project showing how to combine **Retrieval-Augmented
Generation (RAG)** with **answer caching** in a Spring Boot application.

> **Important:** This README describes the intended behavior of the
> endpoints shown below. If your controller uses different
> request/response fields or paths, adjust the examples to match your
> code.

## 1. What this application demonstrates

-   **RAG:** retrieves relevant document content from a vector store and
    supplies it as context to the language model.
-   **Ollama:** runs the chat model and embedding model locally.
-   **Answer cache:** saves answers for previously asked questions so
    repeated questions can avoid another vector search and LLM call.
-   **Document ingestion:** adds document content to the vector store
    for later retrieval.
-   **Cache statistics and clearing:** lets you inspect and reset the
    answer cache.

### Request flow

``` text
User question
     |
     v
Check answer cache
     |
     +---- Cache HIT ----> Return saved answer
     |
     +---- Cache MISS ---> Retrieve relevant document chunks
                                |
                                v
                         Build RAG prompt
                                |
                                v
                         Call Ollama LLM
                                |
                                v
                         Cache the answer
                                |
                                v
                         Return answer
```

The **vector store** and **answer cache** are different:

  -----------------------------------------------------------------------
  Component               Stores                  Purpose
  ----------------------- ----------------------- -----------------------
  Vector store            Document chunks and     Find relevant content
                          embeddings              for a question

  Answer cache            Previously generated    Avoid repeating work
                          answers                 for an identical cached
                                                  question

  Ollama                  Local AI models         Create embeddings and
                                                  generate answers
  -----------------------------------------------------------------------

## 2. Prerequisites

Install and verify:

-   Java 21
-   Maven
-   Ollama
-   The Ollama chat model `llama3.2`
-   The Ollama embedding model `nomic-embed-text`

Check Java and Maven:

``` cmd
java -version
mvn -version
```

Check that Ollama is running:

``` cmd
curl.exe http://localhost:11434/api/tags
```

If the models are not downloaded, run these commands in Windows Command
Prompt:

``` cmd
ollama pull llama3.2
ollama pull nomic-embed-text
```

Confirm that both models appear in the Ollama tags response.

## 3. Configuration

Example `src/main/resources/application.properties`:

``` properties
spring.application.name=rag-cache-spring-ai-ollama
server.port=8080

spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
spring.ai.ollama.embedding.options.model=nomic-embed-text
```

Use the Spring AI BOM version configured by the project. If you are
using Spring AI `2.0.0`, keep all Spring AI dependencies aligned to that
version through the BOM and verify that the artifact names and APIs
match that release.

For a local demo, an in-memory `SimpleVectorStore` is suitable. It is
not a persistent production vector database: indexed documents are lost
when the application restarts unless your application reloads them.

## 4. Build and start the application

Open Windows Command Prompt in the project folder, for example:

``` cmd
cd /d D:\micro\usecase_task\rag-cache-spring-ai-ollama
```

Build:

``` cmd
mvn -U clean package
```

Start:

``` cmd
mvn spring-boot:run
```

Or run the generated JAR from the `target` folder, using its actual
filename:

``` cmd
java -jar target\YOUR-APPLICATION.jar
```

The application should listen on `http://localhost:8080`.

## 5. Windows CMD: API examples

These commands are for **Windows Command Prompt (CMD)**, not PowerShell.
Use `curl.exe` and normal double quotes around URLs.

### 5.1 Check Ollama

``` cmd
curl.exe http://localhost:11434/api/tags
```

This checks the Ollama API and lists downloaded models. It does not
verify that the Spring Boot application is running.

### 5.2 Ask a RAG question

``` cmd
curl.exe -X POST "http://localhost:8080/api/rag/ask" -H "Content-Type: application/json" -d "{\"question\":\"What is the refund policy for a duplicate payment?\"}"
```

The first request for a question that is not cached should normally be a
**cache MISS**. The service should retrieve relevant document content,
call the LLM, store the answer in the cache, and return the answer.

### 5.3 Ask about refund processing

``` cmd
curl.exe -X POST "http://localhost:8080/api/rag/ask" -H "Content-Type: application/json" -d "{\"question\":\"How long does an approved refund normally take?\"}"
```

If this question has not been cached, it should also be a cache MISS.
The returned answer depends on the documents that have actually been
indexed and retrieved.

### 5.4 Add a document

``` cmd
curl.exe -X POST "http://localhost:8080/api/rag/documents" -H "Content-Type: application/json" -d "{\"title\":\"Refund exceptions\",\"content\":\"Duplicate payments require transaction verification and approval. Approved refunds normally take 5 to 7 business days.\"}"
```

The document endpoint should embed and add the content to the vector
store. The exact response depends on the controller implementation.

**Note:** Adding a document does not automatically update an existing
cached answer unless your code explicitly invalidates the relevant cache
entries. For a simple demo, clear the answer cache after changing
documents.

### 5.5 Check answer-cache statistics

``` cmd
curl.exe http://localhost:8080/api/rag/cache/stats
```

The response format depends on your implementation. If the endpoint
exposes hit and miss counters, use them to observe whether the repeated
question was served from cache.

### 5.6 Clear the answer cache

``` cmd
curl.exe -X DELETE "http://localhost:8080/api/rag/cache"
```

This should clear the **answer cache only**, not the vector store,
unless your implementation explicitly does both.

## 6. Observe how caching works

Run the following sequence in order.

1.  Ask the same question once using section 5.2.
2.  Ask the exact same question again using the same command.
3.  Check cache statistics using section 5.5.
4.  Clear the cache using section 5.6.
5.  Ask the original question again.
6.  Check statistics again.

Expected behavior when the cache is correctly implemented:

  Request                               Expected cache result   Retrieval and LLM
  ------------------------------------- ----------------------- -------------------
  First request for a new question      MISS                    Runs
  Exact same question again             HIT                     Skipped
  After clearing the cache, ask again   MISS                    Runs
  A different, uncached question        MISS                    Runs

This is expected behavior, not a guarantee about exact counter values.
Statistics may include earlier requests, and the implementation may
normalize question text before creating cache keys.

### What counts as the same question?

If the cache key is the raw question string, these may be different
keys:

-   `What is the refund policy?`
-   `what is the refund policy?`
-   `What is the refund policy?`

If your code normalizes case and whitespace, some variations may share a
cache entry. Check the cache-key logic to know which behavior applies.

## 7. Logging to prove a cache hit

Add logging around the cache lookup and the code that performs retrieval
and calls the LLM. For example, adapt these messages to your service:

``` java
log.info("Cache HIT for question: {}", question);
log.info("Cache MISS for question: {}", question);
log.info("Retrieving relevant documents from vector store");
log.info("Calling Ollama to generate an answer");
```

The expected sequence is:

**First request**

``` text
Cache MISS
Retrieving relevant documents from vector store
Calling Ollama to generate an answer
```

**Repeated identical request**

``` text
Cache HIT
```

If the logs show retrieval or an LLM call on every request, your cache
may not wrap the full RAG operation, the key may differ, or caching may
not be enabled as expected.

## 8. Important design considerations

### Cache invalidation after document updates

When documents change, a previously cached answer can become stale.
Clear the answer cache after ingestion for this demo, or implement
targeted invalidation/versioned cache keys in a more complete
application.

### User and tenant isolation

If answers depend on user identity, access permissions, tenant, locale,
or other request context, include that context in the cache key.
Otherwise, one user's cached answer could be returned to another user
when that is not appropriate.

### Cache lifetime and memory

Configure an expiry policy and a maximum cache size for long-running
applications. Without limits, cached entries may use more memory over
time.

### In-memory storage

An in-memory answer cache and `SimpleVectorStore` are local to the
running process. They are useful for learning and demos, but their
contents do not survive a restart. For production, consider a persistent
vector database and an appropriate shared/distributed cache if required.

### Do not confuse answer caching with embedding caching

-   **Answer caching:** stores the final answer for a question.
-   **Embedding caching:** stores vectors for repeated text so embedding
    generation can be avoided.
-   **Vector store:** stores document vectors and metadata for
    similarity search.

They solve different performance problems.

## 9. Troubleshooting

### `No qualifying bean of type 'VectorStore'`

Spring has no `VectorStore` bean to inject. Configure a supported vector
store bean or add the correct store starter and configuration for the
Spring AI version used by the project. Merely adding the advisor
dependency does not create the vector store.

### `No qualifying bean of type 'EmbeddingModel'`

Verify the Ollama model starter and embedding configuration are present,
and that the configured embedding model is available in Ollama.

### Ollama connection refused

Start Ollama and verify:

``` cmd
curl.exe http://localhost:11434/api/tags
```

### The answer does not reflect a newly added document

Check that the document endpoint actually embeds and stores the
document, that the question retrieves the new content, and that a stale
answer-cache entry is not being returned. Clear the answer cache and try
again.

### Repeated questions still call Ollama

Check that: - The cache lookup occurs before retrieval and generation. -
The same cache instance is used for reads and writes. - The key is
stable for repeated requests. - The cache entry has not expired or been
evicted. - The code returns the cached value on a hit instead of
continuing through the RAG pipeline.

### Endpoint returns 404

Verify the controller mappings. The sample commands assume these paths
exist:

-   `POST /api/rag/ask`
-   `POST /api/rag/documents`
-   `GET /api/rag/cache/stats`
-   `DELETE /api/rag/cache`

## 10. Summary

RAG helps the model answer using retrieved document content. Answer
caching helps avoid repeating the same work for repeated questions. To
demonstrate the difference, ask a question twice, inspect cache
statistics and logs, clear the cache, and ask it again.
