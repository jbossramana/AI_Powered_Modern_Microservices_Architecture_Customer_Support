# Spring AI EmbeddingService with Ollama

A complete REST example showing how to generate text embeddings with Spring Boot, Spring AI, and a local Ollama embedding model.

## What this example does

1. Accepts text through a REST endpoint.
2. Calls Spring AI's `EmbeddingModel`.
3. Generates a numeric vector using Ollama's `nomic-embed-text` model.
4. Returns the original text, vector dimension count, and vector values.

**This is an embedding-generation example, not a complete RAG application.** It does not store vectors in a vector database or perform similarity search.

## Technology

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Ollama
- `nomic-embed-text`

Use versions available in your environment's Maven repositories. If a specified version is not published or available to your repository, choose a compatible released Spring Boot/Spring AI pair.

## Prerequisites

Install:
- JDK 21
- Maven 3.9+
- Ollama

## 1. Download and start the embedding model

In a terminal:

```bash
ollama pull nomic-embed-text
ollama list
```

Ollama normally serves requests at `http://localhost:11434`. Start Ollama before running the Spring application.

## 2. Run the application

From the directory containing `pom.xml`:

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

## 3. Test from Windows Command Prompt

Use `curl.exe` in Windows CMD:

```bat
curl.exe -X POST "http://localhost:8080/api/embeddings" -H "Content-Type: application/json" -d "{\"text\":\"I cannot log in to my account\"}"
```

Example response shape (the numeric values are illustrative; actual output comes from your local model):

```json
{
  "text": "I cannot log in to my account",
  "dimensions": 768,
  "vector": [0.0123, -0.0456, 0.0789]
}
```

The response contains the complete vector, so the vector shown above is abbreviated for illustration. The exact dimensions depend on the model/version; check the actual response.

## 4. Test with a different question

```bat
curl.exe -X POST "http://localhost:8080/api/embeddings" -H "Content-Type: application/json" -d "{\"text\":\"I forgot my password\"}"
```

The resulting vector represents the second text. Similarity between the two vectors can be calculated separately; generating embeddings alone does not automatically compare them.

## 5. Health check

```bat
curl.exe "http://localhost:8080/actuator/health"
```

## Project structure

```text
spring-ai-embedding-service/
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/example/embedding
    │   │   ├── EmbeddingApplication.java
    │   │   ├── api/EmbeddingController.java
    │   │   └── service/EmbeddingService.java
    │   └── resources/application.yml
    └── test/java/com/example/embedding/EmbeddingApplicationTests.java
```

## How the code works

### `EmbeddingService`

Spring injects an `EmbeddingModel` bean configured by the Spring AI Ollama starter.

```java
float[] vector = embeddingModel.embed(text);
```

- `text` is the source sentence.
- `embed(text)` calls the configured embedding model.
- `float[]` is the resulting vector.
- Each number is one dimension of the learned representation.

The service checks that input is not null or blank. It does not assume that individual vector dimensions have human-readable meanings.

### `EmbeddingController`

The controller exposes `POST /api/embeddings`, validates the request, calls the service, and returns JSON.

Request:

```json
{"text":"I cannot log in to my account"}
```

### `application.yml`

`spring.ai.ollama.embedding.options.model` selects the embedding model. The chat model setting is not needed for this embedding-only example.

## Common issues

### Connection refused to localhost:11434
Make sure Ollama is running. Check it with:

```bat
ollama list
```

### Model not found
Pull it first:

```bat
ollama pull nomic-embed-text
```

### Maven cannot resolve a dependency/version
Verify that the requested Spring Boot and Spring AI versions are published and compatible in your configured repositories. Use the matching Spring AI starter and BOM for the selected release.

### Response is large
Embedding vectors contain many floating-point values. In production, you may want to return only the dimensions or a vector identifier rather than exposing the full vector over the API.

## Next step: semantic search / RAG

A typical RAG workflow extends this example:

1. Split documents into chunks.
2. Generate an embedding for each chunk.
3. Store each vector and its text/metadata in a vector store.
4. Embed the user's query.
5. Retrieve the most similar chunks.
6. Pass the retrieved text to an LLM to produce a grounded answer.

A vector database and a chat model are not required just to generate an embedding.
