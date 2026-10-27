# Customer Support Query Transformation RAG

Demo project: Spring Boot 4.1.x, Spring AI 2.0.x, Ollama, `RewriteQueryTransformer`, and an in-memory `SimpleVectorStore`.

> Verify exact Spring Boot / Spring AI patch compatibility and API signatures for the versions available in your Maven repository. This package has not been built against your local environment.

## Requirements
- JDK 21+
- Maven (or Maven Wrapper if you add one)
- Ollama running at `http://localhost:11434`

## 1. Download local models (Windows CMD)
```bat
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
ollama list
```

## 2. Run
From the project root:
```bat
mvn -U clean package
mvn spring-boot:run
```

The application listens on port 8080. Ollama must be running in another terminal/service.

## 3. Test with Windows CMD
```bat
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"My payment was deducted twice for one order. How can I get my money back?\"}"
```

Other examples:
```bat
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"I returned my product last week, but where is my refund?\"}"

curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"Money was deducted, but my order still shows payment pending.\"}"
```

## Request flow
1. `RewriteQueryTransformer` rewrites the user's question.
The key point is that RewriteQueryTransformer uses an LLM to rewrite the customer's question. You provide the LLM through chatClientBuilder, and Spring AI supplies the transformation instructions internally.

2. `VectorStore.similaritySearch` searches using the transformed query.
3. Retrieved document text is passed to the chat model as context.
4. The LLM produces a grounded response.
5. The endpoint returns the original question, transformed query, document count, source metadata, and answer.

## Important limitations
- `SimpleVectorStore` is in-memory: documents are loaded on startup and lost on restart.
- The knowledge-base content is illustrative sample data, not a real business policy.
- This is a learning demo, not production-ready. Add persistent storage, authentication, authorization, retrieval evaluation, observability, and policy-safe escalation before deployment.
- The Spring AI transformer imports/method signatures may vary by exact release. If compilation fails, align them to the reference docs for the exact Spring AI 2.0.x version you use.
