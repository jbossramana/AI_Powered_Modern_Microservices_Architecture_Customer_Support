# Plain Chat (Spring AI)

The smallest possible Spring AI app: one dependency, one bean, one endpoint. No tools,
no memory, no RAG yet — just a `ChatClient` talking to a local Ollama model. Everything
later in this series (`01-tools-spring-ai`, `02-agent-spring-ai`, ...) builds on exactly
this skeleton.

## What this teaches

- The `spring-ai-starter-model-ollama` autoconfiguration (one dependency → a working `ChatModel`)
- The `ChatClient` fluent API: `chatClient.prompt().user(...).call().content()`
- Setting a durable system prompt with `.defaultSystem(...)`

## Prerequisites

1. **Java 21+** and **Maven 3.9+**
2. **[Ollama](https://ollama.com)** installed and running (`ollama serve`, or just open the app)
3. Pull a model that's good at following instructions:
   ```bash
   ollama pull qwen3:8b
   ```
   `qwen3:8b` is used across this whole series because it also supports tool/function
   calling, which you'll need from Module 1 onward — worth pulling once now. Any Ollama
   chat model will work for this module alone if you'd rather use something you already have
   (edit `OLLAMA_CHAT_MODEL` below).

## Run it

```bash
cd 00-chat-spring-ai
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Override the model or Ollama URL without
touching code:

```bash
OLLAMA_CHAT_MODEL=llama3.1:8b OLLAMA_BASE_URL=http://localhost:11434 mvn spring-boot:run
```

## Try it

curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" -d "{\"message\":\"How do I reset my password?\"}" 

## How it works

- `SupportChatConfig` builds a `ChatClient` from the autoconfigured `ChatModel` and
  attaches a system prompt (`.defaultSystem(...)`) so every request is grounded as an
  Acme Cloud Storage support agent, not a generic chatbot.
- `ChatController` is a two-line REST wrapper: take a message in, call the model, hand
  the text back out.

