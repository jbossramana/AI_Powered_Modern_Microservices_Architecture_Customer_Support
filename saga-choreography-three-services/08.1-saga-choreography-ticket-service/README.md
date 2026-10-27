# 08.1 Saga Choreography Ticket Service

This ZIP contains the **Ticket Service**, which starts the Saga and listens to billing outcomes. It also includes the complete cross-service setup/test guide below.

# Kafka Saga Choreography — Windows Guide

This README is intentionally self-contained: it explains the whole three-service flow, Kafka topics, Windows CMD commands, and test cases.

## 1. What this example demonstrates

A refund is a distributed business transaction implemented with **Saga choreography**. There is no separate orchestrator. Each service reacts to Kafka events and publishes the next event.

| Service | Port | Responsibility |
|---|---:|---|
| Ticket Service | 8081 | REST entry point; holds ticket; reacts to billing outcomes; starts compensation when requested; publishes final outcome |
| Billing Service | 8082 | Consumes ticket events; applies refund policy; processes or reverses a refund |
| Communication Service | 8083 | Consumes the final outcome; uses Ollama/LLM to draft a customer-facing message |

## 2. Event flow

### Normal approval

```text
Windows curl
   |
   v
Ticket Service :8081
   | hold ticket; create sagaId
   | TicketRefundHeld
   v
Kafka topic: ticket-events
   |
   v
Billing Service :8082
   | if amount <= 10,000 cents: process refund
   | RefundProcessed
   v
Kafka topic: billing-events
   |
   v
Ticket Service :8081
   | mark ticket RESOLVED; Saga status APPROVED
   | TicketRefundResolved -> ticket-events
   | RefundOutcomeReady  -> refund-outcomes
   v
Communication Service :8083
   | Ollama LLM drafts customer message
   v
GET /internal/communications
```

### Natural decline

Amounts greater than `10,000` cents (`$100.00` in this demo) are declined for manual review. Billing publishes `RefundDeclined` to `billing-events`; Ticket Service changes the ticket to `REFUND_DECLINED`, marks the Saga `DECLINED`, and publishes `RefundOutcomeReady` to `refund-outcomes`.

### Compensation test

When `simulateFailureAfterBilling` is `true`, Billing first processes the refund and publishes `RefundProcessed`. Ticket Service deliberately refuses to confirm the ticket, publishing `TicketResolutionFailed` to `ticket-events`. Billing consumes it, reverses the refund, and publishes `RefundReversed` to `billing-events`. Ticket Service then declines the ticket and publishes the final outcome to `refund-outcomes`.

## 3. Kafka topics — create them on Windows

Prerequisites: Kafka is running and reachable at `localhost:9092`. For your Kafka installation, open CMD and run:

```bat
cd C:\kafka_2.13-4.3.1\bin\windows
kafka-broker-api-versions.bat --bootstrap-server localhost:9092
```

If the broker command succeeds, create the three topics (one partition and one replica are suitable for a single-broker local demo):

```bat
kafka-topics.bat --bootstrap-server localhost:9092 --create --if-not-exists --topic ticket-events --partitions 1 --replication-factor 1
kafka-topics.bat --bootstrap-server localhost:9092 --create --if-not-exists --topic billing-events --partitions 1 --replication-factor 1
kafka-topics.bat --bootstrap-server localhost:9092 --create --if-not-exists --topic refund-outcomes --partitions 1 --replication-factor 1
```

Verify:

```bat
kafka-topics.bat --bootstrap-server localhost:9092 --list
kafka-topics.bat --bootstrap-server localhost:9092 --describe --topic ticket-events
kafka-topics.bat --bootstrap-server localhost:9092 --describe --topic billing-events
kafka-topics.bat --bootstrap-server localhost:9092 --describe --topic refund-outcomes
```

Expected topic names:

| Topic | Producer | Consumer | Events |
|---|---|---|---|
| `ticket-events` | Ticket Service | Billing Service | `TicketRefundHeld`, `TicketResolutionFailed`, plus ticket lifecycle events |
| `billing-events` | Billing Service | Ticket Service | `RefundProcessed`, `RefundDeclined`, `RefundReversed` |
| `refund-outcomes` | Ticket Service | Communication Service | `RefundOutcomeReady` |

**Important fix in this updated project:** Ticket Service routes `RefundOutcomeReady` to `refund-outcomes`; all other ticket events go to `ticket-events`. The publisher waits up to 10 seconds for broker acknowledgement and logs the destination topic, so a successful log entry means Kafka acknowledged the send.

## 4. Prerequisites

- JDK 21
- Maven installed and available as `mvn`
- Kafka reachable at `localhost:9092`
- Ollama installed and running at `http://localhost:11434`

Pull the communication model before starting the services:

```bat
ollama pull llama3.2:latest
```

You can use a different Ollama chat model by setting `OLLAMA_CHAT_MODEL` before launching the Communication Service. Ensure the chosen model is available in Ollama.

## 5. Start the services in three CMD windows

Unzip all three project ZIPs into separate folders. In each CMD window, `cd` to the extracted folder containing that project's `pom.xml`, then run the corresponding command.

**CMD window 1 — Ticket Service**

```bat
cd C:\path\to\08.1-saga-choreography-ticket-service
mvn spring-boot:run
```

**CMD window 2 — Billing Service**

```bat
cd C:\path\to\08.2-saga-choreography-billing-service
mvn spring-boot:run
```

**CMD window 3 — Communication Service**

```bat
cd C:\path\to\08.3-saga-choreography-communication-service
mvn spring-boot:run
```

Wait for all three applications to finish starting. The application logs should show each service's listening port. Keep the terminals open because their logs are useful for tracing each event.

## 6. Test from Windows Command Prompt (CMD)

All commands below are **single-line Windows CMD commands**. Use `curl.exe`, not PowerShell's `curl` alias. They do not require `jq`.

### Test A — happy path / approved refund

Ticket `TCK-2001` starts as `OPEN`. Request a refund of 999 cents ($9.99):

```bat
curl.exe -i -X POST "http://localhost:8081/api/tickets/TCK-2001/refund" -H "Content-Type: application/json" -d "{\"amountCents\":999,\"simulateFailureAfterBilling\":false}"
```

Expected initial response: `202 Accepted`, with a generated `sagaId` and `status` `PROCESSING`. **Copy the actual sagaId from the response.** The API only confirms that processing started.

After a second or two, poll the result (replace the placeholder with the returned UUID):

```bat
curl.exe -s "http://localhost:8081/api/tickets/TCK-2001/refund/PASTE_SAGA_ID_HERE"
```

Expected final Saga status: `APPROVED`.

Check ticket status:

```bat
curl.exe -s "http://localhost:8081/internal/tickets/TCK-2001"
```

Expected ticket status: `RESOLVED`.

Check the AI-drafted communication:

```bat
curl.exe -s "http://localhost:8083/internal/communications"
```

Expected: a JSON array with a message for this Saga. It can remain empty if the final event has not arrived yet, or if Communication Service/Ollama logs an error.

### Test B — natural decline by billing policy

Ticket `TCK-2002` starts as `OPEN`. Ask for 15,000 cents ($150), which exceeds the demo's $100 automatic approval limit:

```bat
curl.exe -i -X POST "http://localhost:8081/api/tickets/TCK-2002/refund" -H "Content-Type: application/json" -d "{\"amountCents\":15000,\"simulateFailureAfterBilling\":false}"
```

Copy the returned `sagaId` and poll:

```bat
curl.exe -s "http://localhost:8081/api/tickets/TCK-2002/refund/PASTE_SAGA_ID_HERE"
```

Expected final Saga status: `DECLINED`.

Check the ticket and communication list:

```bat
curl.exe -s "http://localhost:8081/internal/tickets/TCK-2002"
curl.exe -s "http://localhost:8083/internal/communications"
```

Expected ticket status: `REFUND_DECLINED`. The communication list should eventually include the final decline message.

### Test C — compensation after billing processed the refund

The local `TicketStore` is in memory. If you already ran Test A, `TCK-2001` is now `RESOLVED` and cannot be reused without resetting Ticket Service. **Stop and restart only Ticket Service** (Ctrl+C in its CMD window, then run `mvn spring-boot:run` again) after previous tests have settled. This resets its sample tickets and in-memory Saga state. Keep Kafka and the other services running.

Then run:

```bat
curl.exe -i -X POST "http://localhost:8081/api/tickets/TCK-2001/refund" -H "Content-Type: application/json" -d "{\"amountCents\":500,\"simulateFailureAfterBilling\":true}"
```

Copy the new `sagaId` and poll:

```bat
curl.exe -s "http://localhost:8081/api/tickets/TCK-2001/refund/PASTE_SAGA_ID_HERE"
curl.exe -s "http://localhost:8081/internal/tickets/TCK-2001"
```

Expected final Saga status: `DECLINED`; ticket status: `REFUND_DECLINED`. In Billing Service logs, look for a processed refund followed by the compensation/reversal. The generated `refundId` is included in `RefundProcessed` and can be found in the `billing-events` topic; use it to check:

```bat
curl.exe -s "http://localhost:8082/internal/refunds/PASTE_REFUND_ID_HERE"
```

Expected refund status: `REVERSED`.

## 7. Debug Kafka events on Windows

Use a separate CMD window. This shows messages in a topic from the beginning and stops after the timeout if no further records arrive:

```bat
cd C:\kafka_2.13-4.3.1\bin\windows
kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic ticket-events --from-beginning --timeout-ms 10000
```

For the other topics, replace `ticket-events` with `billing-events` or `refund-outcomes`.

To filter for a particular Saga ID, pipe to `findstr` (replace the UUID):

```bat
kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic ticket-events --from-beginning --timeout-ms 10000 | findstr /C:"PASTE_SAGA_ID_HERE"
```

Repeat for `billing-events` and `refund-outcomes`. A log line such as `Reconfiguration failed: No configuration found` from the local Kafka Log4j configuration is not, by itself, proof that the broker failed. Check whether the broker command succeeds and whether the consumer displays records.

## 8. Troubleshooting

- **HTTP 400:** Make sure the CMD JSON body uses the exact backslash-escaped double quotes shown above. The request record requires a positive `amountCents`; the updated controller enables `@Positive` validation.
- **HTTP 409 Conflict:** The ticket is no longer `OPEN`; use the other seeded ticket or restart Ticket Service to reset its in-memory sample data.
- **Saga stuck at `PROCESSING`:** Check logs in Ticket and Billing Service. Confirm `TicketRefundHeld` in `ticket-events`, then `RefundProcessed`/`RefundDeclined` in `billing-events`. Producer logs now include the topic and broker acknowledgement.
- **Communications remains `[]`:** Confirm Ticket Service publishes `RefundOutcomeReady` to `refund-outcomes`, verify the Communication Service is consuming that topic, and check its logs for Ollama/LLM errors. Run `ollama list` and confirm `llama3.2:latest` is available.
- **No Kafka records:** Check broker reachability with `kafka-broker-api-versions.bat --bootstrap-server localhost:9092`, verify all three topics exist, and inspect the producer's failure log.
- **Old Saga still stuck after code changes:** Fixing event routing does not retroactively move an already-published event to another topic or replay an already committed event. Use a fresh test flow after restarting Ticket Service; inspect the old messages in Kafka before deciding whether to reset topics/consumer offsets.

## 9. Important demo limitations

- Ticket, Saga, refund, and communication stores are in-memory; restarting a service resets that service's local records.
- Kafka messages and consumer offsets persist independently of the in-memory stores.
- The communication service's “delivery” is recorded in memory for demo purposes; it does not send a real email or SMS.
- The example uses one broker, one partition per topic, plain JSON, and no dead-letter topic. It is an educational example, not production-ready saga infrastructure.
