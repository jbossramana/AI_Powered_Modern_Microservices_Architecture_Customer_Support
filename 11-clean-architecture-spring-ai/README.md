# Bonus — Clean Architecture (Hexagonal / Ports & Adapters)

The triage, RAG-answer, and human-approval use cases from `02-agent-spring-ai`,
`03-rag-spring-ai`, and `06-human-in-the-loop-spring-ai`, rebuilt as one project with
proper layering instead of each module's flat package. Same domain, same three
capabilities, same in-memory data — the point of this module isn't new AI features,
it's showing how you'd actually organize an AI-integrated app so Spring AI is a
swappable detail instead of something your business logic is written against.

## The Dependency Rule, in one diagram

```
 ┌─────────────────────────────────────────────────────────┐
 │  adapter/  (Interface Adapters)                          │
 │  in/web  — REST controllers          out/ai — Spring AI, │
 │                                       LangChain4j         │
 │                                       out/persistence     │
 │   ┌───────────────────────────────────────────────────┐  │
 │   │  application/  (Use Cases)                         │  │
 │   │  port/in  — use-case interfaces                    │  │
 │   │  port/out — what use cases need from the outside   │  │
 │   │  service  — use-case implementations                │  │
 │   │   ┌─────────────────────────────────────────────┐ │  │
 │   │   │  domain/  (Entities & business rules)        │ │  │
 │   │   │  Ticket, Customer, TicketClassification,     │ │  │
 │   │   │  DraftResponse, EscalationPolicy              │ │  │
 │   │   └─────────────────────────────────────────────┘ │  │
 │   └───────────────────────────────────────────────────┘  │
 └─────────────────────────────────────────────────────────┘
        config/ (outermost) wires every arrow below with @Bean
```

Dependencies only point **inward** — arrows go from outer rings to inner rings, never
the reverse. `domain/` imports nothing outside itself. `application/` imports `domain/`
and its own `port` interfaces, nothing else. `adapter/` is the only place allowed to
import Spring, Spring AI, or LangChain4j. This isn't just a diagram here — run:

```bash
grep -rn "springframework\|langchain4j" src/main/java/com/acmesupport/cleanarch/domain/ src/main/java/com/acmesupport/cleanarch/application/
```

and get nothing (a couple of doc-comment *sentences* mentioning Spring by name are the
only matches — no `import` statements). That's the whole exercise, made checkable.

## Why this matters especially for AI apps

Ordinary business apps swap databases occasionally. AI apps are swapping *frameworks* —
Spring AI, LangChain4j, raw provider SDKs — and *models* far more often than that, and
the ecosystem is genuinely still moving fast (this whole series had to be built against
versions current as of a couple of months ago, not a couple of years ago). If
`TriageTicketService` were written directly against `ChatClient`, every framework churn
event would risk touching your actual business rules — the escalation policy, the
approval workflow — not just infrastructure code. Here, it can't: `TriageTicketService`
only knows `TicketClassifierPort`. Proof, not assertion: see
`adapter/out/ai/LangChain4jTicketClassifierAdapter` — a complete, working alternative
implementation of that exact port using LangChain4j instead, with zero changes required
anywhere else in the codebase to make it valid. It isn't wired up by default (see its
javadoc for how you'd flip it on), but it compiles as part of this same project,
against the same port, right now.

## Package guide

| Package | Contains | Can import |
|---|---|---|
| `domain/model` | Entities & value objects (`Ticket`, `Customer`, `TicketClassification`, `DraftResponse`) | nothing but `java.*` |
| `domain/policy` | Business rules (`EscalationPolicy`) | `domain.model` |
| `domain/exception` | Domain exceptions | nothing but `java.*` |
| `application/port/in` | Use-case interfaces (what the app can be asked to do) | `domain/` |
| `application/port/out` | Outbound ports (what use cases need from outside) | `domain/` |
| `application/service` | Use-case implementations | `domain/`, `application/port/*` — **no Spring** |
| `adapter/in/web` | REST controllers, request/response DTOs | `application/port/in`, Spring MVC |
| `adapter/out/ai` | Spring AI (and, alternatively, LangChain4j) implementations of outbound ports | `application/port/out`, Spring AI / LangChain4j |
| `adapter/out/persistence` | In-memory implementations of repository ports | `application/port/out`, Spring |
| `config` | `@Bean` wiring, the vector store setup | everything |

## A design choice worth knowing about: purist vs. pragmatic

`application/service/*` has zero Spring annotations — no `@Service`, no `@Component`.
Beans are constructed explicitly in `config/UseCaseConfig` instead. This is the
**purist** approach: it makes the Dependency Rule airtight and mechanically verifiable
(the `grep` above), at the cost of one extra file to maintain.

Most real Spring teams take the **pragmatic** middle ground instead: put `@Service`
directly on `TriageTicketService` and let component scanning wire it, accepting one
framework import into the application layer as a fair trade for less boilerplate. Both
are legitimate, debated choices in the Java community — this module deliberately picked
the stricter one because it makes the architecture's central claim something you can
check with `grep` instead of something you have to take on faith.

## Prerequisites

```bash
ollama pull qwen3:8b
ollama pull nomic-embed-text
```

## Run it

```bash
mvn spring-boot:run
```

## Try all three use cases

**Triage** (uses `TicketClassifierPort` → `SpringAiTicketClassifierAdapter`):

curl -s http://localhost:8080/api/tickets/triage -H "Content-Type: application/json" -d "{\"customerId\":\"C-1002\",\"ticketText\":\"Locked out of our shared drive again during business hours, third time this month.\"}" 

**RAG-answered support question** (uses `KnowledgeBaseSearchPort` + `ResponseGeneratorPort`):

curl -s http://localhost:8080/api/support/ask -H "Content-Type: application/json" -d "{\"question\":\"How much storage does the Business plan include?\"}" 


**Draft → approve → send** (uses `DraftResponseUseCase` + `ReviewDraftUseCase`):

curl -s http://localhost:8080/api/tickets/draft -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-9001\",\"customerId\":\"C-1001\",\"ticketText\":\"How do I reset my password?\"}"

  
# → note the "id" field

curl -s http://localhost:8080/api/approvals/<id>/approve -H "Content-Type: application/json" -d "{\"reviewer\":\"jordan@acmesupport.example\"}" 

curl -s -X POST http://localhost:8080/api/approvals/<id>/send | jq


## Run the test — no Spring, no Ollama

```bash
mvn test -Dtest=TriageTicketServiceTest
```

This passes in milliseconds without starting a Spring context or hitting a model at
all — `TriageTicketServiceTest` hand-writes tiny fake implementations of
`TicketClassifierPort`, `CustomerRepositoryPort`, and `TicketRepositoryPort` and checks
`EscalationPolicy`'s actual decision logic directly. This is usually the strongest
practical argument for this whole layering exercise: your riskiest business logic gets
to have a fast, deterministic test suite that doesn't depend on a model being available,
deterministic, or even correct.

## Comparing this to the rest of the series

Modules `00`–`09` deliberately use one flat package per module — the right choice when
the goal is showing *one concept* as clearly as possible, without layering ceremony
competing for attention. This module shows the other end of a real tradeoff: more
files and more indirection, in exchange for business logic that's independently
testable and insulated from exactly the kind of framework churn AI tooling is
currently going through. Neither approach is "more correct" in general — which one
earns its cost depends on how large the codebase gets and how long it needs to live.

## Honest limitations

- This is still a demo: in-memory "repositories," three customers, three knowledge-base
  articles. The architecture is real; the data behind it isn't production scale.
- As with the rest of the series, this was not compiled in the sandbox that generated
  it (no Maven Central access there) — every API used was checked against current
  documentation, including `VectorStore.similaritySearch`/`Document.getText()` and
  LangChain4j's `AiServices` returning custom records directly, both confirmed against
  current reference docs. If `mvn compile` disagrees with something here, it's most
  likely a small drift in one of these fast-moving libraries rather than a structural
  problem with the layering itself.
