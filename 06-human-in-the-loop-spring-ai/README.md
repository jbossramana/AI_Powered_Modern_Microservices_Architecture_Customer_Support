# Step 6 — Human-in-the-Loop Approval (Spring AI)

Every prior module returned an AI-generated answer straight to the caller. This module
refuses to do that: drafting and sending are two separate steps with a mandatory human
approval gate in between, enforced in code, not just convention.

## What this teaches

- Splitting "generate" from "act" into separate endpoints/steps
- A minimal approval-queue state machine: `PENDING → APPROVED/REJECTED → SENT`
- Enforcing the gate at the **service layer** (`DraftAndApprovalService.send()` throws
  unless status is `APPROVED`) so it can't be bypassed by calling things in the wrong
  order from some other endpoint
- Letting a reviewer edit before approving, not just accept/reject

## Prerequisites

```bash
ollama pull qwen3:8b
```

## Run it

```bash
cd 06-human-in-the-loop-spring-ai
mvn spring-boot:run
```

## Try the full lifecycle

**1. Draft a reply** (status starts as `PENDING`):

curl -s http://localhost:8080/api/tickets/draft -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-3001\",\"customerId\":\"C-1001\",\"ticketText\":\"Can I get a refund for last month, I forgot to cancel before renewal?\"}" 


Note the `id` in the response - you'll need it below.

**2. See it waiting for review:**

curl -s "http://localhost:8080/api/approvals?status=PENDING"


**3a. Approve as-is:**

curl -s http://localhost:8080/api/approvals/{id}/approve -H "Content-Type: application/json" -d "{\"reviewer\":\"jordan@acmesupport.example\"}" 


**3b. ...or approve with an edit** (same endpoint, add `editedText`):

curl -s http://localhost:8080/api/approvals/{id}/approve -H "Content-Type: application/json" -d "{\"reviewer\":\"jordan@acmesupport.example\",\"editedText\":\"Hi! I have gone ahead and refunded last month's charge - you'll see it in 3-5 business days.\"}" 


**4. Send it** (only works now that it's `APPROVED`):

curl -s -X POST http://localhost:8080/api/approvals/{id}/send

**Try sending an unapproved item** to see the guard in action:

curl -s -i -X POST http://localhost:8080/api/approvals/{some-still-pending-id}/send
# -> 409 Conflict, "Cannot send item ...: status is PENDING, not APPROVED"


## How it works

- `PendingApproval` is an immutable record; each review action (`approve`, `reject`,
  `markSent`) returns a new instance rather than mutating one in place, so there's never
  ambiguity mid-review about which version is current.
- `DraftAndApprovalService` holds the queue (an in-memory map — swap for a real
  datastore in production) and is the *only* place that can move an item to `SENT`,
  and only when status is already `APPROVED`.
- `ApprovalController` is a thin REST wrapper with one endpoint per lifecycle step.

## Where this fits with the rest of the series

This module is intentionally standalone, but in a real system it's the natural
next stop after Module 5's graph: instead of `draft_response` being the end of that
graph, it could write into this same approval queue and stop there — a human decision
becomes the actual terminal step for anything that reaches a customer. LangGraph4j
supports this directly via checkpointing and interrupts if you want to explore merging
the two; this series keeps them separate so each concept is easy to study in isolation.

## What's next

`07-ai-gateway-spring-ai` steps back from the support-ticket domain entirely to look at
a cross-cutting concern: putting a gateway in front of *any* of these AI calls for
authentication, rate limiting, and caching.
