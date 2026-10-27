# Saga Pattern — Orchestration (4 Microservices + LLM)

A refund request that touches three independent services, coordinated by a fourth: the
**orchestrator**. This is the Saga pattern's answer to a question ordinary transactions
can't: how do you keep data consistent across separate services (and separate
databases) when you can't wrap them all in one ACID transaction? Answer: each service
commits its own local transaction, and if a later step fails, the orchestrator calls
each already-completed step's **compensating action** to undo it, in reverse order.

The LLM's job (`communication-service`, using Spring AI + Ollama) is to draft the
customer-facing outcome message — approved or declined — as the saga's unconditional
final step.

## The four services

| Service | Port | Role |
|---|---|---|
| `saga-orchestrator-service` | 8080 | Coordinates the other three. The only customer-facing service. |
| `ticket-service` | 8081 | Participant. Holds/releases/resolves a ticket. |
| `billing-service` | 8082 | Participant. Processes/reverses a refund, enforces a $100 auto-approval policy. |
| `communication-service` | 8083 | Participant. Drafts the outcome message with an LLM, "delivers" it. |

## The saga

```
                         POST /api/refund-requests
                                    |
                                    v
                        ┌───────────────────────┐
                        │  saga-orchestrator     │
                        └───────────────────────┘
                                    |
        1. hold-ticket ─────────> ticket-service        (compensate: release hold)
                                    |
        2. process-refund ───────> billing-service       (compensate: reverse refund)
                                    |
        3. confirm-ticket-resolved > ticket-service       (compensate: none needed)
                                    |
                     ── success ───┴─── any step fails ──
                     |                                   |
              outcome = APPROVED                 compensate completed steps,
                                                  in reverse order
                     |                                   |
                     └─────────────┬─────────────────────┘
                                    v
        4. draft + deliver ──────> communication-service  (always runs; not compensatable)
                                    |
                                    v
                          response to the caller
```

Steps 1–3 are the compensatable chain. Step 4 always runs exactly once, regardless of
outcome, because drafting text has no side effect worth undoing — see
`CommunicationServiceApplication`'s javadoc for that reasoning spelled out.

## Orchestration vs. choreography, briefly

This is an **orchestration** saga: one service (the orchestrator) knows the entire
sequence and explicitly calls each participant in order. The alternative,
**choreography**, has each service publish an event when it finishes and react to
events from others - no single place holds the whole flow. Orchestration trades some
coupling (participants must expose the operations the orchestrator expects) for a saga
that's easy to read start-to-finish in one file (`RefundSagaOrchestrator`) and easy to
test as a unit; choreography trades that visibility for looser coupling between
services, usually at the cost of the overall flow being spread across many event
handlers with no single source of truth. Neither is strictly better - it depends on how
many participants are involved and how often the flow itself changes.

## Prerequisites

```bash
ollama pull qwen3:8b
```

Only `communication-service` talks to Ollama.

## Run all four

Four terminals:
```bash
cd ticket-service && mvn spring-boot:run
cd billing-service && mvn spring-boot:run
cd communication-service && mvn spring-boot:run
cd saga-orchestrator-service && mvn spring-boot:run
```
Order doesn't matter except that the orchestrator's first request needs all three
participants already up.

## Test scenario 1 — happy path

curl -s http://localhost:8080/api/refund-requests -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"amountCents\":999,\"simulateFailureAfterBilling\":false}"

Expect `"outcome": "APPROVED"`, `"completedSteps"` listing all three steps,
`"compensatedSteps": []`, and a `message` confirming the $9.99 refund. Verify directly:

```bash
curl -s http://localhost:8081/internal/tickets/TCK-2001 | jq .status   # "RESOLVED"
curl -s http://localhost:8083/internal/communications | jq             # the delivered message
```

## Test scenario 2 — natural decline (amount exceeds policy)

curl -s http://localhost:8080/api/refund-requests -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2002\",\"amountCents\":15000,\"simulateFailureAfterBilling\":false}"

`$150.00` exceeds billing-service's $100 automatic-approval limit. Expect
`"outcome": "DECLINED"`, `"completedSteps": ["hold-ticket"]` only (billing rejected
before a refund record ever existed), `"compensatedSteps": ["hold-ticket"]`, and a
message explaining the limit and offering manual review. Verify:

```bash
curl -s http://localhost:8081/internal/tickets/TCK-2002 | jq .status   # "REFUND_DECLINED"
```

## Test scenario 3 — full compensation (failure *after* money moves)

This is the scenario that matters most for understanding sagas: what happens when a
refund has already gone through and something *afterward* fails.

curl -s http://localhost:8080/api/refund-requests -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"amountCents\":500,\"simulateFailureAfterBilling\":true}"


`simulateFailureAfterBilling` is a test-only hook (see `ConfirmTicketResolvedStep`) -
there's no natural business rule in this demo that makes the final confirmation step
fail on its own, so this flag exists to make the full compensation path reachable o
demand. Expect `"completedSteps": ["hold-ticket", "process-refund"]`,
`"compensatedSteps": ["process-refund", "hold-ticket"]` (reverse order), and note the
refund ID from the response:

```bash
curl -s http://localhost:8082/internal/refunds/<refundId> | jq .status   # "REVERSED"
curl -s http://localhost:8081/internal/tickets/TCK-2001 | jq .status     # "REFUND_DECLINED"
```

The money moved, then moved back. That reversal is the whole point of the pattern.

## What's simplified here, on purpose

- **Synchronous REST, not messaging.** Real saga orchestrators often communicate with
  participants over a message broker (Kafka, RabbitMQ) so a temporarily-down
  participant doesn't block the whole saga and retries are durable across orchestrator
  restarts. Plain `RestClient` calls keep `RefundSagaOrchestrator` readable top to
  bottom without a broker to also stand up for this example.
- **In-memory saga log.** `RefundSagaOrchestrator.sagaLog` doesn't survive a restart. A
  production orchestrator persists saga state (which steps completed) so it can resume
  or compensate correctly if the orchestrator process itself crashes mid-saga.
- **No idempotency keys.** Calling `/api/refund-requests` twice for the same ticket
  starts two independent sagas. A production system would generate/accept an
  idempotency key so retries don't double-process.
- **A failed compensation is only logged**, not retried or escalated. `catch` around
  each `compensate()` call in `RefundSagaOrchestrator` says exactly why in a comment:
  this is a real, unsolved edge case sagas don't eliminate on their own.
