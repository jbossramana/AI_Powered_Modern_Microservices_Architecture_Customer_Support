# Customer Support RAG — Spring Boot + Spring AI + Ollama

A learning/demo project showing Retrieval-Augmented Generation (RAG) over public website pages with:

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Ollama local chat model: `llama3.2`
- Ollama embedding model: `nomic-embed-text`
- Spring AI `SimpleVectorStore` (in-memory; no database)
- Jsoup for website HTML extraction
- REST API for ingestion and chat

> Version note: this sample pins Spring Boot `4.1.0` and Spring AI `2.0.0`. If your Maven repository or environment does not provide these exact versions, use the compatible released patch versions documented by the Spring projects. The source has not been executed against your local environment.

## Prerequisites

1. JDK 21
2. Maven 3.9+
3. Ollama installed and running
4. Internet access to fetch the website pages you configure

## 1. Download Ollama models (Windows CMD or PowerShell)

```bat
ollama pull llama3.2
ollama pull nomic-embed-text
ollama list
```

Ollama normally listens on `http://localhost:11434`.

## 2. Configure website URLs

Edit `src\main\resources\application.yml` and replace `https://example.com/` with website pages you are authorized to index, for example your company's FAQ, returns, shipping, and warranty pages.

Use a small list of specific pages for this demo. This sample does not crawl every link on a domain.

## 3. Run the application (Windows CMD)

From the folder containing `pom.xml`:

```bat
mvnw.cmd spring-boot:run
```

If the project does not include Maven Wrapper scripts, install Maven and run:

```bat
mvn spring-boot:run
```

Check health:

```bat
curl.exe http://localhost:8080/api/support/health
```

Expected shape:

```json
{"status":"UP","service":"customer-support-rag"}
```

## 4. Index the configured website pages

```bat
curl.exe -X POST http://localhost:8080/api/support/ingest
```

This fetches each configured page, extracts text, splits it into chunks, creates embeddings through Ollama, and stores them in `SimpleVectorStore`.

Example response shape:

```json
{
  "status": "completed",
  "pagesRequested": 1,
  "chunksIndexed": 2,
  "urls": ["https://example.com/"]
}
```

The number of chunks depends on page content and the splitter. The example values above are illustrative.

## 5. Ask a customer support question (Windows CMD)

```bat
curl.exe -X POST http://localhost:8080/api/support/chat -H "Content-Type: application/json" -d "{\"question\":\"What information is available on this website?\"}"
```

PowerShell alternative:

```powershell
$body = @{ question = "What information is available on this website?" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/support/chat" -Method Post -ContentType "application/json" -Body $body
```

The answer depends on the pages you indexed. `example.com` is a placeholder site and will not contain your company's policies.

## 6. Important behavior

- Ingestion and chat must use the same running application instance because `SimpleVectorStore` stores vectors in memory.
- Restarting the app clears the vector store; run the ingestion endpoint again after each restart.
- `SimpleVectorStore` is for learning and small demos, not large production indexes.
- This sample indexes configured URLs only. It does not implement recursive crawling, scheduled re-indexing, authentication, or citation formatting.
- Do not expose arbitrary URL fetching to anonymous users. Restrict URLs to trusted domains in production to reduce SSRF risk.
- Respect website terms, robots directives, rate limits, and access restrictions.
- Retrieval is not a guarantee of correctness. Validate the returned answer and add source links, monitoring, and fallback-to-human support before production use.

## API summary

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/support/health` | Health check |
| POST | `/api/support/ingest` | Ingest configured website URLs |
| POST | `/api/support/chat` | Ask a question using RAG |

Chat request JSON:

```json
{
  "question": "What is your return policy?"
}
```

## Troubleshooting

### Connection refused to Ollama

- Ensure Ollama is installed and running.
- Run `ollama list`.
- Check `http://localhost:11434`.
- If Spring Boot runs in Docker, `localhost` points to the container, not the host.

### Model not found

```bat
ollama pull llama3.2
ollama pull nomic-embed-text
```

Ensure model names in `application.yml` match the models returned by `ollama list`.

### Website ingestion fails

- Confirm the URL is correct and publicly accessible.
- Check whether the site blocks automated requests or requires JavaScript rendering.
- Start with a simple HTML page and review the application logs.

### Answers are unrelated

- Index more relevant FAQ or policy pages.
- Ask specific questions matching the website content.
- Remember that the vector store is in memory and must be populated after each application restart.

## Project structure

```text
customer-support-rag-ollama/
├── pom.xml
├── README.md
├── docs/
│   └── CONCEPTS.md
└── src/
    └── main/
        ├── java/com/example/support/
        │   ├── CustomerSupportRagApplication.java
        │   ├── config/VectorStoreConfig.java
        │   ├── controller/SupportController.java
        │   └── service/
        │       ├── CustomerSupportService.java
        │       └── WebsiteIngestionService.java
        └── resources/application.yml
```
