# Spring AI Enterprise Feature Lab — Spring Boot 4.1.0 + Spring AI 2.0.0 + Ollama

Java 21 / Maven learning project demonstrating Spring AI `ChatClient`, system prompts, local Ollama, conversation memory, Java tool calling, typed ticket triage, grounded knowledge retrieval, knowledge ingestion, validation, Actuator health, and unit tests.

**Requested versions:** Spring Boot `4.1.0`, Spring AI `2.0.0`. Spring AI 2.0 is designed for Spring Boot 4.x. See the [Spring Boot 4.1.0 announcement](https://spring.io/blog/2026/06/10/spring-boot-4/) and [Spring AI 2.0.0 announcement](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/).

## 1. Requirements

- JDK 21
- Maven 3.9+
- Ollama for Windows: https://ollama.com/download

## 2. Download a local model

Open **Windows Command Prompt (`cmd.exe`)**:

```cmd
ollama pull llama3.2
ollama list
```

Ollama normally runs as a background/desktop service. If `ollama list` works, you usually do not need a second server. Default endpoint: `http://localhost:11434`.

If required, run this in its own terminal:

```cmd
ollama serve
```

## 3. Build and run

Extract the ZIP and open Command Prompt in the folder containing `pom.xml`:

```cmd
java -version
mvn -version
mvn clean test
mvn spring-boot:run
```

API base URL: `http://localhost:8080`.

To use another port:

```cmd
set SERVER_PORT=8082
mvn spring-boot:run
```

Update the URLs below if you change the port.

## 4. Windows CMD curl commands

These commands are for **Command Prompt (`cmd.exe`)**, not PowerShell. Copy each command as one line. JSON double quotes are escaped as `\"`.

### A. Basic chat

```cmd
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"How do I reset my password?\"}"
```

### B. Conversation memory

Run the following commands in order, using the same session ID:

```cmd
curl.exe -X POST "http://localhost:8080/api/chat/demo-session" -H "Content-Type: application/json" -d "{\"message\":\"My name is Ravi. Remember it for this conversation.\"}"
curl.exe -X POST "http://localhost:8080/api/chat/demo-session" -H "Content-Type: application/json" -d "{\"message\":\"What is my name?\"}"
```

Memory is in-process and clears when the app restarts. Use different session IDs for separate conversations.

### C. Java tool calling

```cmd
curl.exe -X POST "http://localhost:8080/api/chat/tools" -H "Content-Type: application/json" -d "{\"message\":\"What are your support hours?\"}"
curl.exe -X POST "http://localhost:8080/api/chat/tools" -H "Content-Type: application/json" -d "{\"message\":\"Look up the status of ticket TCK-1002\"}"
```

`SupportTools` exposes two Java methods using `@Tool`. These are sample values, not a live ticketing integration.

### D. Grounded support answer (RAG-style)

```cmd
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"What should I do if my account may be compromised?\"}"
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"What is the office parking policy?\"}"
```

The app retrieves up to three keyword-matching snippets, asks the model to answer only from that context, and returns the answer plus source objects. No matches produce a deterministic fallback without a model call.

**Scope:** The retriever is a simple in-memory keyword example, not embedding/vector RAG. Production RAG normally adds chunking, embeddings, a vector store, metadata filters, relevance thresholds, reranking, and evaluation.

### E. Add a knowledge-base entry

```cmd
curl.exe -X POST "http://localhost:8080/api/knowledge" -H "Content-Type: application/json" -d "{\"id\":\"mfa\",\"title\":\"MFA setup\",\"text\":\"To set up MFA, open Security Settings and follow the authenticator enrollment steps. Never share one-time codes with support staff.\"}"
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"How do I set up MFA?\"}"
curl.exe "http://localhost:8080/api/knowledge"
```

Knowledge entries are in memory and disappear on restart. IDs must be unique.

### F. Structured ticket triage

```cmd
curl.exe -X POST "http://localhost:8080/api/tickets/triage" -H "Content-Type: application/json" -d "{\"message\":\"Three production regions are unavailable after deployment and customers cannot log in.\"}"
```

Spring AI maps the response to a Java `TicketTriage` record. Always validate model-generated values before making operational decisions.

### G. Health check

```cmd
curl.exe "http://localhost:8080/actuator/health"
```

Expected response when healthy:

```json
{"status":"UP"}
```

## 5. Endpoint summary

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/chat` | Basic chat using `ChatClient` |
| POST | `/api/chat/{sessionId}` | Session-based memory |
| POST | `/api/chat/tools` | Chat with registered Java tools |
| POST | `/api/support/ask` | Keyword retrieval + grounded answer |
| POST | `/api/knowledge` | Add an in-memory knowledge entry |
| GET | `/api/knowledge` | List knowledge entries |
| POST | `/api/tickets/triage` | Typed ticket triage |
| GET | `/actuator/health` | Health endpoint |

## 6. Troubleshooting

- **Connection refused on port 8080:** start Spring Boot and verify the configured port.
- **Connection refused on port 11434:** start Ollama or run `ollama serve`.
- **Model not found:** run `ollama pull llama3.2` and verify with `ollama list`.
- **Maven resolution error:** verify JDK 21, internet access to Maven Central, then run `mvn -U clean test`.
- **CMD JSON parse errors:** paste commands as one line into `cmd.exe`; do not use PowerShell backticks.
- **Slow or inconsistent model responses:** try a different local model by setting `OLLAMA_CHAT_MODEL` before starting the application.
- **Duplicate knowledge ID:** use a new ID or restart the application.

## 7. Concepts demonstrated

- `ChatClient` and system/user prompts
- Local Ollama model integration with no cloud API key
- `MessageChatMemoryAdvisor` and in-memory `ChatMemory`
- `@Tool`-annotated Java methods
- Typed response mapping with `.entity(TicketTriage.class)`
- Retrieval-augmented prompt construction, source return, and no-match fallback
- Spring MVC validation, configuration, Actuator, and unit tests

This is a teaching sample, not a production-ready service. Add authentication, authorization, rate limiting, observability, persistent stores, retrieval evaluation, and approval gates for high-impact actions before production use.
