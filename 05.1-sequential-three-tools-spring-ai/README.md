# Sequential Three-Tool Agent — Spring Boot 4.1.0 + Spring AI 2.0.0

This project demonstrates an LLM-led investigation where **exactly one tool is executed per LLM turn**. The tool result is returned to the next LLM request, and the model prints a progress message before selecting the next tool.

## Prerequisites
- JDK 21
- Maven 3.9+
- Ollama for Windows
- Model: `qwen2.5:7b`

```cmd
ollama pull qwen2.5:7b
ollama list
curl.exe http://localhost:11434/api/tags
```

## Project structure

```text
sequential-three-tools-spring-ai/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/sequentialtools/
    │   ├── SequentialToolsApplication.java
    │   ├── data/MockOrderDataRepository.java
    │   ├── model/InvestigationModels.java
    │   ├── service/SequentialAgentService.java
    │   ├── service/SequentialToolExecutor.java
    │   ├── tools/CustomerSupportTools.java
    │   └── web/InvestigationController.java
    └── resources/application.properties
```

## Clear class responsibilities
- `SequentialToolsApplication`: Spring Boot entry point.
- `InvestigationController`: REST endpoints.
- `SequentialAgentService`: calls the LLM once per turn, prints each model message/action, and controls the bounded loop.
- `CustomerSupportTools`: the three read-only tool methods (`checkPayment`, `checkOrder`, `checkInventory`), annotated with Spring AI `@Tool`.
- `SequentialToolExecutor`: Java allowlist that dispatches exactly one selected tool.
- `MockOrderDataRepository`: mock payment, order/shipment and inventory data.
- `InvestigationModels`: request, structured next-action decision, observations and response records.

## Architecture note
To make each step visible, this sample uses **structured LLM output to select one tool name per turn**, then Java validates and invokes that `@Tool` method. It does not use Spring AI's automatic tool-calling loop; Java dispatches one tool at a time. After the result is logged, it is included in the next LLM prompt, where the LLM explains the finding and chooses the next action. This is an explicit, inspectable orchestration pattern.

## Mock data
| Order ID | Payment | Order status | Shipment | Inventory |
|---|---|---|---|---|
| `ORD-101` | PAID, ₹2,500 | CONFIRMED | DELAYED | IN_STOCK, 8 |
| `ORD-102` | FAILED, ₹799 | PENDING_PAYMENT | NOT_SHIPPED | IN_STOCK, 24 |
| `ORD-103` | PAID, ₹1,200 | CONFIRMED | SHIPPED | LOW_STOCK, 2 |

Only in-memory mock data is used. No real-world action is performed.

## Build and run (Windows CMD)
Extract the ZIP and open CMD in the directory containing `pom.xml`:

```cmd
java -version
mvn -version
dir pom.xml
mvn clean package
mvn spring-boot:run
```

Keep the server terminal open and use a second CMD window for curl commands.

## Test URLs and Windows CMD curl commands

### 1. Health check
```cmd
curl.exe -i http://localhost:8080/api/health
```

### 2. Investigate ORD-101 (all three tools)
```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-101\",\"question\":\"My payment was deducted but delivery is delayed. Check payment, order status and inventory one by one.\",\"maxToolCalls\":3}"
```

### 3. Investigate failed payment ORD-102
```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-102\",\"question\":\"Why is my order not shipped? Investigate each system sequentially.\",\"maxToolCalls\":3}"
```

### 4. Limit the run to one tool
```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-101\",\"question\":\"Check my order.\",\"maxToolCalls\":1}"
```

### 5. Another order
```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-103\",\"question\":\"Check payment, shipping and stock.\",\"maxToolCalls\":3}"
```

## What to watch in the console
The exact wording/order can vary by model, but the console includes entries like:

```text
----- LLM TURN 1 -----
[APP -> LLM] Send current observations; request exactly ONE next action.
[LLM MESSAGE] I will verify the payment first.
[LLM REQUEST] CHECK_PAYMENT
[SPRING APP] Dispatching exactly one tool.
[TOOL START] CHECK_PAYMENT("ORD-101")
[TOOL RESULT] PaymentData[orderId=ORD-101, status=PAID, amount=2500.00, transactionId=TXN-101]
[TOOL END] CHECK_PAYMENT
[SPRING APP -> LLM] Result appended. Request next message/action now.

----- LLM TURN 2 -----
[LLM MESSAGE] Payment is confirmed. I will inspect the order status next.
[LLM REQUEST] CHECK_ORDER
[TOOL START] CHECK_ORDER("ORD-101")
[TOOL RESULT] OrderData[orderId=ORD-101, status=CONFIRMED, shipmentStatus=DELAYED, trackingId=TRK-101]

----- LLM TURN 3 -----
[LLM MESSAGE] The order is delayed. I will check inventory next.
[LLM REQUEST] CHECK_INVENTORY
[TOOL RESULT] InventoryData[orderId=ORD-101, sku=SKU-LAPTOP-01, availability=IN_STOCK, availableQuantity=8]

----- LLM TURN 4 -----
[LLM MESSAGE] All checks are complete. Here is the summary.
[LLM REQUEST] DONE
```

These lines illustrate the console format; actual model wording may vary. Java prevents repeated tool calls and rejects unknown tool names. The loop is bounded to three tool calls. If structured output fails, a deterministic fallback chooses the next unexecuted tool.

## Request/response shape
Request JSON:
```json
{"orderId":"ORD-101","question":"Why is delivery delayed?","maxToolCalls":3}
```
Response contains `orderId`, `finalMessage`, `toolCalls`, `totalToolCalls`, and `completed`.

## Troubleshooting

### HTTP 405
Stop any old application with `Ctrl+C`. Confirm the current folder contains this project's `pom.xml`, then restart:
```cmd
mvn clean spring-boot:run
curl.exe -i http://localhost:8080/api/health
```

### Ollama unavailable
```cmd
curl.exe http://localhost:11434/api/tags
ollama list
```
Start the Ollama desktop app if needed.

### Port 8080 in use
```cmd
netstat -ano | findstr :8080
```
Identify the PID before stopping any process.

### Maven build failure
```cmd
java -version
mvn -version
mvn -U clean package
```
Ensure Maven uses JDK 21 and can access Maven Central.

## Production note
This is an educational sample. Before connecting real systems, add authentication, authorization, validation, audit logging, timeouts, retries, observability and human approval for sensitive actions. Treat LLM output as untrusted input.
