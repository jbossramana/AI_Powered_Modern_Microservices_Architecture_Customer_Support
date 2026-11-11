# LangChain4j Enterprise AI Feature Lab (Spring Boot)

This is an expanded, runnable learning project based on the original `00-chat-langchain4j` ZIP. It keeps the original plain-chat endpoint and adds examples of AI Services, system/user prompts, per-session chat memory, Java tool calling, typed ticket triage, knowledge-base retrieval/RAG, ingestion, validation, provider selection, Actuator health, and automated tests.

> **Scope and accuracy:** This is an educational starter, not a production-ready enterprise platform. The included RAG retriever is a lightweight in-memory keyword retriever so it works locally without a database or embedding download. It is not semantic/vector RAG. The notes explain how to replace it with LangChain4j embedding models and a persistent vector store. Memory and knowledge are in process and reset on restart. The ticket tool uses a hard-coded demo dataset only.

## 1. Prerequisites

- JDK 21
- Maven 3.9+
- Ollama installed and running, or an OpenAI API key
- For the default provider, pull a model:

```bash
ollama pull llama3.2:latest
ollama serve
```

If Ollama is already running as a desktop service, you do not need to run `ollama serve` a second time.

## 2. Start the application

From the extracted `00-chat-langchain4j` directory:

```bash
mvn clean test
mvn spring-boot:run
```

Base URL: `http://localhost:8080`. Use a different port if needed:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

### Use OpenAI instead of Ollama (optional)

PowerShell:

```powershell
$env:SUPPORT_PROVIDER="openai"
$env:OPENAI_API_KEY="your-api-key"
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
mvn spring-boot:run
```

Windows CMD:

```bat
set SUPPORT_PROVIDER=openai
set OPENAI_API_KEY=your-api-key
set OPENAI_CHAT_MODEL=gpt-4o-mini
mvn spring-boot:run
```

Linux/macOS:

```bash
export SUPPORT_PROVIDER=openai
export OPENAI_API_KEY='your-api-key'
export OPENAI_CHAT_MODEL='gpt-4o-mini'
mvn spring-boot:run
```

Do not commit API keys to source control. The project chooses a provider through `SUPPORT_PROVIDER`; `ollama` is the default.

## 3. Test with Windows Command Prompt (CMD)

Open **Command Prompt (`cmd.exe`)** in a separate window while the Spring Boot application is running. These examples use `curl.exe`, one-line commands, and CMD-compatible JSON escaping. Copy each command as a single line.

Base URL: `http://localhost:8080`

### A. Basic chat

```cmd
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"How do I reset my password?\"}"
```

### B. Memory across turns

Use the same session ID to continue the same in-memory conversation.

```cmd
curl.exe -X POST "http://localhost:8080/api/chat/demo-session" -H "Content-Type: application/json" -d "{\"message\":\"My name is Ravi. Please remember it for this conversation.\"}"
curl.exe -X POST "http://localhost:8080/api/chat/demo-session" -H "Content-Type: application/json" -d "{\"message\":\"What is my name?\"}"
```

Use a different session ID to start a separate conversation. Memory is held in process and is cleared when the application restarts.

### C. Tool calling

```cmd
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"What are your support hours?\"}"
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Look up the status of ticket TCK-1002\"}"
```

The model may choose a registered Java method based on the tool description. These demo methods return static sample data, not live ticket-system information.

### D. Grounded support answer (RAG-style)

```cmd
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"What should I do if an account may be compromised?\"}"
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"What is the policy for a topic that is not in the knowledge base?\"}"
```

The response contains `answer` and retrieved `sources`. Retrieval uses keyword overlap. If no knowledge snippets match, the endpoint returns a safe fallback without calling the model.

### E. Add a knowledge-base entry and query it

```cmd
curl.exe -X POST "http://localhost:8080/api/knowledge" -H "Content-Type: application/json" -d "{\"id\":\"mfa\",\"title\":\"MFA setup\",\"text\":\"To set up MFA, open Security Settings and follow the authenticator enrollment steps. Never share one-time codes with support staff.\"}"
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"How do I set up MFA?\"}"
curl.exe "http://localhost:8080/api/knowledge"
```

Knowledge entries are held in memory and disappear when the application restarts. If the `mfa` ID already exists, use a different ID or restart the application.

### F. Structured ticket triage

```cmd
curl.exe -X POST "http://localhost:8080/api/tickets/triage" -H "Content-Type: application/json" -d "{\"message\":\"Three production regions are unavailable after the deployment and customers cannot log in.\"}"
```

The endpoint returns a typed triage result. Validate model-generated classifications against real business rules before triggering operational actions.

### G. Health check

```cmd
curl.exe "http://localhost:8080/actuator/health"
```

Expected successful response includes a status similar to:

```json
{"status":"UP"}
```

### H. Demo tool information

```cmd
curl.exe "http://localhost:8080/api/tools/demo"
```

### CMD quoting tips

- Keep each `curl.exe` command on **one line** in Command Prompt.
- In JSON passed to `-d`, escape inner double quotes as `\\\"` in Markdown source so the displayed command contains `\"`.
- Do not use PowerShell backticks or Bash backslashes for line continuation in CMD.
- If you see `Connection refused`, verify that Spring Boot is running and that the port matches the URL.
- To change the server port, set `SERVER_PORT` before starting the app, then update `http://localhost:8080` in these examples.

## 4. Endpoint overview

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/chat` | Stateless AI Service chat; tools available |
| POST | `/api/chat/{sessionId}` | Chat with session-specific sliding-window memory |
| POST | `/api/support/ask` | Retrieve knowledge snippets and generate grounded answer |
| POST | `/api/knowledge` | Add an in-memory knowledge item |
| GET | `/api/knowledge` | List in-memory knowledge items |
| POST | `/api/tickets/triage` | Return a typed ticket triage result |
| GET | `/actuator/health` | Application health |

## 5. Troubleshooting

- **Connection refused to localhost:11434:** start Ollama and verify `ollama list` works.
- **Model not found:** run `ollama pull llama3.2:latest` or set `OLLAMA_CHAT_MODEL` to an installed model.
- **Port 8080 busy:** use `--server.port=8082` and change the test URLs accordingly.
- **OpenAI authentication error:** confirm `SUPPORT_PROVIDER=openai` and that `OPENAI_API_KEY` is set in the same terminal.
- **Maven dependency resolution:** run `mvn -U clean test` with internet access and JDK 21.
- **Unexpected model output:** model quality varies; try a stronger model, lower temperature, and validate outputs server-side.

## 6. Read these companion files

- `NOTES.md` — concepts, request flow, design details, production hardening and extension roadmap.
- `LANGCHAIN4J_VS_SPRING_AI.md` — separate comparison, decision guidance and code-shape examples.
- `TESTING.md` — copy/paste test checklist and expected behavior.
