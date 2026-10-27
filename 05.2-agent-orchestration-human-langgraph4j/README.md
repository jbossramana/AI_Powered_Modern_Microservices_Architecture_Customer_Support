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

```text
                         START
                           |
                         triage
                      /          \
       urgent/high + negative    other tickets
                  |                   |
               escalate       knowledge_retrieval
                  |                   |
                  |             draft_response
                  |                   |
                  +---------+---------+
                            |
                     human_approval
                            |
                           END
                            |
                   durable review record
                  (demo uses in-memory queue)

        REST reviewer decision: PENDING -> APPROVED / REJECTED
```

**Every ticket enters the review queue.** Routine tickets have `actionType: RESPONSE_DRAFT`; urgent/negative tickets have `actionType: ESCALATION` and an `escalationReason`. Approving an escalation acknowledges/accepts the escalation for human handling; it does not send a message. Approving a response draft records the approval, but this demo has no customer-send operation.

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

**Routine ticket** (draft is held for human approval; it is not sent automatically):

curl -s http://localhost:8080/api/tickets/process -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"customerId\":\"C-1001\",\"ticketText\":\"Quick question - what happens to my files if I go over my storage limit?\"}"


**Urgent, angry ticket** (should escalate):

curl -s http://localhost:8080/api/tickets/process -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"customerId\":\"C-1001\",\"ticketText\":\"This is the third outage this week and nobody has responded. We are moving our whole team off this platform if this is not fixed today.\"}" 


Both routine and urgent tickets are added to the pending review queue. Routine tickets return `route: AWAITING_HUMAN_APPROVAL`; urgent/negative tickets return `route: AWAITING_HUMAN_ESCALATION_REVIEW`. Both return `approvalStatus: PENDING`. The demo never sends a customer message.

**List all pending human reviews (drafts and escalations):**

```bash
curl -s http://localhost:8080/api/tickets/approvals/pending
```

**Approve a pending review:** (for `RESPONSE_DRAFT`, this records approval of the draft; for `ESCALATION`, it acknowledges the escalation for human handling)

```bash
curl -s -X POST http://localhost:8080/api/tickets/TCK-2001/approval -H "Content-Type: application/json" -d "{\"decision\":\"APPROVED\",\"reviewer\":\"agent-17\",\"comment\":\"Reviewed and ready to send\"}"
```

**Reject a draft:**

```bash
curl -s -X POST http://localhost:8080/api/tickets/TCK-2001/approval -H "Content-Type: application/json" -d "{\"decision\":\"REJECTED\",\"reviewer\":\"agent-17\",\"comment\":\"Please correct the troubleshooting steps\"}"
```

**Check one approval:**

```bash
curl -s http://localhost:8080/api/tickets/TCK-2001/approval
```

The API rejects duplicate decisions and unknown ticket approvals. The pending endpoint includes both response drafts and escalation reviews, distinguished by `actionType`. `ApprovalQueue` is intentionally in-memory for this teaching sample: pending decisions disappear on restart, so use a database or durable queue and add authentication/authorization before production.

Compare `actionType`, `draftResponse`, and `escalationReason` — one of the two branches runs, and both converge on `human_approval` before a review record is created.

## How it works

- `SupportGraphState` (in `graph/`) is the state every node reads and writes, via a
  `Map<String,Object>` returned from each node's `apply(...)`.
- Five `NodeAction<SupportGraphState>` implementations — `TriageNode`,
  `KnowledgeRetrievalNode`, `ResponseDraftNode`, `EscalationNode`, and
  `HumanApprovalNode` — are the workflow steps. Each is a Spring-managed component.
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

## Human approval safety boundary

No customer-facing send operation exists in this sample. Approval records the human decision only. In a production implementation, put the actual send operation behind a separate service that requires an `APPROVED` record and verifies the reviewer's authorization server-side. The included queue is in-memory and is for learning/demo use only.
