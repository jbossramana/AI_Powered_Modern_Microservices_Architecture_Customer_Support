# Notes — Spring AI 2.0.0 + Spring Boot 4.1.0

## Architecture
HTTP request -> Spring MVC controller -> Spring AI ChatClient -> Ollama model API -> local Ollama model -> response.

## ChatClient and prompts
`ChatClient` is Spring AI's fluent interface for model interaction. The app sets a default system prompt and passes each request as user content. Prompt instructions guide behavior but are not a security boundary.

## Tool calling
`SupportTools` exposes methods with `@Tool`. The `/api/chat/tools` endpoint registers the component for that request. The model can request a tool call, the framework invokes the Java method, and the result is sent back to the model. Demo tools only return static sample data.

## Conversation memory
`MessageWindowChatMemory` and `InMemoryChatMemoryRepository` store a bounded message window. `MessageChatMemoryAdvisor` adds prior conversation messages based on `ChatMemory.CONVERSATION_ID`. The sample uses the URL session ID. Production systems should authenticate users and authorize session access.

## RAG-style retrieval
1. Search the in-memory knowledge list using keyword overlap.
2. Select up to three snippets.
3. Add them to the model prompt as reference data.
4. Ask the model to answer only from context and cite snippet IDs.
5. Return the answer and source objects.
6. Return a safe fallback when no snippet matches.

This is not semantic/vector RAG. A production version commonly adds document loading, chunking, embeddings, a vector database, metadata filters, reranking, citations, and retrieval evaluation.

## Typed ticket triage
`.entity(TicketTriage.class)` maps the model output into a Java record. Typed mapping helps response shape, but does not prove that classifications are correct. Validate allowed values and actions before using them.

## Configuration
- `OLLAMA_BASE_URL` — default `http://localhost:11434`
- `OLLAMA_CHAT_MODEL` — default `llama3.2`
- `OLLAMA_TEMPERATURE` — default `0.2`
- `SERVER_PORT` — default `8080`

## Production hardening
Add authentication/authorization, secrets management, input and output validation, rate limits, timeouts, persistent stores, safe tool allowlists, prompt-injection defenses, observability, evaluation, and human approval for high-impact actions.
