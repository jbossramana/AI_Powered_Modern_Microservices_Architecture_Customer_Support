# Spring AI + Ollama Document Summarizer

A minimal Spring Boot REST API that accepts a `.txt` document and returns an LLM-generated summary using Spring AI and a local Ollama model.

## Requirements

- Java 21
- Maven 3.9+
- Ollama installed and running
- Model `qwen2.5:7b`

## 1. Download the model

```bash
ollama pull qwen2.5:7b
```

Ensure Ollama is running at `http://localhost:11434`.

## 2. Run the application

From this directory:

```bash
mvn spring-boot:run
```

The API listens on port 8080.

## 3. Summarize the sample document

In a second terminal, from the project directory:

```bash
curl -X POST http://localhost:8080/api/documents/summarize -F "file=@D:/micro/usecase_task/13-spring-ai-llm-document-summarizer/sample-documents/customer-complaint.txt"
```

The response is plain text containing the generated summary.

## API

- **Method:** `POST`
- **URL:** `/api/documents/summarize`
- **Content-Type:** `multipart/form-data`
- **Form field:** `file`
- **Supported file type:** `.txt`

## How it works

1. The controller validates and reads the uploaded text file.
2. `DocumentSummaryService` creates a prompt using Spring AI `ChatClient`.
3. Spring AI sends the prompt to Ollama.
4. The local model generates a summary.
5. The API returns the summary as text.

## Configuration

See `src/main/resources/application.properties`. The default model is `qwen2.5:7b` at `http://localhost:11434`.

## Notes

- This starter example supports plain-text `.txt` files only; PDF/DOCX parsing is not included.
- The project has not been compiled or executed in this environment. If your Maven repository cannot resolve the specified Spring Boot or Spring AI versions, use versions available from the official Spring repositories and compatible with each other.
- For production, add authentication, stronger file/content validation, timeouts, observability, and request/text-size limits.
