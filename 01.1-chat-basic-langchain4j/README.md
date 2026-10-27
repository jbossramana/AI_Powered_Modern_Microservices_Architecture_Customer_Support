# Plain Chat (LangChain4j)

The exact same use case as `00-chat-spring-ai` — one endpoint, one system prompt, one
local Ollama model — implemented with **LangChain4j** instead. Run both side by side to
see how the two frameworks differ in philosophy before you commit to one for the rest of
the series (this series uses Spring AI from Module 1 onward, as requested, but it's worth
seeing the alternative once).

## What this teaches

- LangChain4j's **AI Service** pattern(AI-powered business capability): declare a plain interface (`SupportAssistant`)
  with `@SystemMessage`, and `AiServices.create(...)` generates the implementation for you
  at runtime — no Spring Boot autoconfiguration involved, just a factory call.
- Wiring `OllamaChatModel` by hand, which makes every constructor argument explicit.

## Spring AI vs. LangChain4j, in this module

| | Spring AI (`00-chat-spring-ai`) | LangChain4j (this module) |
|---|---|---|
| Model setup | Autoconfigured from `application.yml` | Built explicitly in `AssistantConfig` |
| Conversation contract | Fluent builder: `.prompt().user(x).call().content()` | Declarative interface: `assistant.chat(x)` |
| System prompt | `.defaultSystem(...)` on the `ChatClient` | `@SystemMessage` on the interface method |
| Best fit | Spring-native apps that want convention over configuration | Framework-agnostic Java (also runs on Quarkus, Micronaut, plain Java) |

## Prerequisites

Same as Module 0a:

```bash
ollama pull llama3.2:latest
```

(Ollama must be running — `ollama serve` or the desktop app.)

## Run it

```bash
cd 00-chat-langchain4j
mvn spring-boot:run
```

Runs on `http://localhost:8080` 

## Try it

curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" -d "{\"message\":\"How do I reset my password?\"}"

## How it works

- `SupportAssistant` is an interface, not a class — `AiServices.create()` in
  `AssistantConfig` generates a dynamic proxy that turns the `@SystemMessage` and the
  method signature into a prompt, calls the model, and returns the plain `String` result.
- `AssistantConfig` builds `OllamaChatModel` explicitly from `application.yml` values
  (`support.ollama.*`) since there's no starter doing it for us.

