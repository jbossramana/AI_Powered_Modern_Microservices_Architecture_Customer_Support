# Step 5 — Agent Orchestration (LangGraph4j + Spring AI)

A single `ChatClient` call can't express "do A, then branch to B or C based on what
happened in A." That's what this module adds: a real graph, with a routing decision
made in code between steps. LangGraph4j owns the *shape* of the workflow (which node
runs next); Spring AI's `ChatClient` still does every actual model call, from inside
plain Spring-managed node classes.

## What this teaches

- Modeling a workflow as a `StateGraph`: nodes, edges, and a **conditional edge** that
  picks the next node based on state a previous node produced
- Defining shared state with `AgentState` + `Channels.base(...)` — simple "last write
  wins" fields, as opposed to an accumulating list like a chat history
- Where Spring AI fits inside a LangGraph4j node: it's just a `ChatClient` call from
  ordinary Java, no special glue required
- Encoding a business-critical routing rule (`routeAfterTriage`) as a plain, testable
  static function instead of trusting a prompt to "always escalate angry tickets"

## The graph

```
        START
          |
       triage  (structured-output classification, same idea as Module 2)
        /    \
 urgent+     everything
 negative      else
    |            |
 escalate   knowledge_retrieval  (simplified retrieval, same idea as Module 3)
    |            |
   END    draft_response  (drafts the customer-facing reply)
                 |
                END
```

## Prerequisites

```bash
ollama pull qwen3:8b
```

## Run it

```bash
cd 05-agent-orchestration-langgraph4j
mvn spring-boot:run
```

## Try it

**Routine ticket** (should auto-draft):

curl -s http://localhost:8080/api/tickets/process -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"customerId\":\"C-1001\",\"ticketText\":\"Quick question - what happens to my files if I go over my storage limit?\"}"


**Urgent, angry ticket** (should escalate):

curl -s http://localhost:8080/api/tickets/process -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2002\",\"customerId\":\"C-1002\",\"ticketText\":\"This is the third outage this week and nobody has responded. We are moving our whole team off this platform if this is not fixed today.\"}" 


Compare the `route` field and notice `draftResponse` vs. `escalationReason` — only one
of the two branches ran for each request; the other field stays at its schema default.

## How it works

- `SupportGraphState` (in `graph/`) is the state every node reads and writes, via a
  `Map<String,Object>` returned from each node's `apply(...)`.
- Four `NodeAction<SupportGraphState>` implementations — `TriageNode`,
  `KnowledgeRetrievalNode`, `ResponseDraftNode`, `EscalationNode` — are the workflow's
  steps. Each is a plain `@Component`; two of them happen to call the shared
  `ChatClient` bean from `OrchestrationChatConfig`, two don't need to.
- `SupportGraphBuilder` wires them into a `StateGraph`, adds the conditional edge with
  `routeAfterTriage` as the routing function, and compiles it into a `CompiledGraph`
  bean that `OrchestrationController` invokes per request.

## A note on the knowledge retrieval node

`KnowledgeRetrievalNode` uses simple keyword overlap against four hardcoded articles,
not the vector store from Module 3 — this keeps the module focused on graph mechanics
rather than repeating RAG setup. In a real system, swap its body for a call through a
`VectorStoreDocumentRetriever` exactly like Module 3's; nothing about the graph's shape
needs to change, since the node's contract (read `ticketText`, write
`retrievedContext`) stays the same either way.

## What's next

`06-human-in-the-loop-spring-ai` builds a proper approval queue for the kind of
AI-drafted response this graph's `draft_response` node produces — nothing here actually
sends anything to a customer yet, on purpose.
