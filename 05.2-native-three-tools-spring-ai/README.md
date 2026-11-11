# Native Spring AI Tool Calling — Three Tools

Stack: Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Ollama `qwen2.5:7b`.

The LLM is given three native tools: `checkPayment`, `checkOrder`, and `checkInventory`. It can request a tool; Spring AI dispatches the matching `@Tool` method, returns the tool result to the model, and continues until the model gives a final answer. Each Java tool logs its invocation and result to the console.

**Important:** The prompt requests one tool at a time, but an LLM/provider may not always obey perfectly. The order and number of calls are model-dependent. If a sequence is mandatory, enforce it in Java. This sample uses read-only mock data.

## Project structure

```text
native-tool-calling/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/nativetools/
    │   ├── NativeToolCallingApplication.java
    │   ├── data/MockOrderRepository.java
    │   ├── model/InvestigationRequest.java
    │   ├── service/OrderInvestigationService.java
    │   ├── tools/OrderInvestigationTools.java
    │   └── web/InvestigationController.java
    └── resources/application.properties
```

## Prerequisites

Install JDK 21, Maven 3.9+, and Ollama for Windows. In Windows CMD:

```cmd
java -version
mvn -version
ollama pull qwen2.5:7b
ollama list
curl.exe http://localhost:11434/api/tags
```

Ensure Ollama is running.

## Build and run

Extract the ZIP. Open CMD in the project directory (where `pom.xml` exists):

```cmd
dir pom.xml
mvn clean package
mvn spring-boot:run
```

Keep the app console open. Open a second CMD window for curl commands.

## Test the API

Health:

```cmd
curl.exe -i http://localhost:8080/api/health
```

Investigate order ORD-101:

```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-101\",\"question\":\"My payment was deducted but delivery is delayed. Please check payment, order status, and inventory one by one.\"}"
```

Investigate failed payment ORD-102:

```cmd
curl.exe -i -X POST http://localhost:8080/api/investigate -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-102\",\"question\":\"Why has my order not shipped? Check payment, order status, and inventory.\"}"
```

## What you should see in the console

```text
NATIVE SPRING AI TOOL-CALLING RUN
[APP -> LLM] Sending question and native tool definitions.
[NATIVE TOOL CALL] checkPayment(orderId=ORD-101)
[TOOL RESULT] Payment[orderId=ORD-101, status=PAID, ...]
[TOOL RETURN] Spring AI returns this result to the LLM.
[NATIVE TOOL CALL] checkOrder(orderId=ORD-101)
[TOOL RESULT] Order[orderId=ORD-101, status=CONFIRMED, shipmentStatus=DELAYED, ...]
[NATIVE TOOL CALL] checkInventory(orderId=ORD-101)
[TOOL RESULT] Inventory[orderId=ORD-101, sku=SKU-LAPTOP-01, status=IN_STOCK, ...]
[FINAL LLM ANSWER]
...
```

This is an illustrative sequence, not a guaranteed transcript. The model may call fewer tools, choose a different order, or occasionally request multiple tools. Tool method logs show what was actually executed.

## How it works

1. `OrderInvestigationService` sends the user request and native tool definitions to the LLM.
2. The LLM returns a structured native tool-call request.
3. Spring AI dispatches the corresponding method in `OrderInvestigationTools`.
4. The method result is returned to the LLM as tool output.
5. The LLM may request another tool or return its final answer.
6. `InvestigationController` returns the answer as JSON.

There is no Java `switch` dispatching tool names and no custom outer loop. The normal tool-call/result interaction is handled by Spring AI's ChatClient/tool-calling machinery.

## Troubleshooting

**HTTP 405:** Stop an old application with Ctrl+C. From this project's directory run `mvn clean spring-boot:run`, then test `curl.exe -i http://localhost:8080/api/health`.

**Ollama connection error:**
```cmd
curl.exe http://localhost:11434/api/tags
ollama list
```

**Port 8080 busy:**
```cmd
netstat -ano | findstr :8080
```
Identify the process before stopping it.

## Production note

Before connecting real systems, add authentication, authorization, input validation, audit logging, timeouts, rate limits, and approval gates for sensitive operations. A system prompt is not a security boundary.
