# Customer Support Agent — Guarded Spring AI Tool Calls

Demo project using Java 21, Spring Boot 4.1.0, Spring AI 2.0.0, Maven, and Ollama.

## What it demonstrates

- The LLM chooses whether to call `checkPaymentStatus` and/or `checkOrderStatus`.
- `GuardedToolCallback` validates the requested tool name and JSON arguments before delegating to the original callback.
- The LLM receives tool results and creates the final answer.
- Demo data is in-memory; this is not a production payment integration.

## Requirements

- JDK 21
- Maven 3.9+
- Ollama installed and running

> Version note: verify that Spring Boot `4.1.0` and Spring AI `2.0.0` are available in your Maven repositories. If either version is not yet published in your environment, Maven cannot resolve the project until you select available compatible releases.

## Run on Windows

Open PowerShell in the project directory:

```powershell
java -version
mvn -version
ollama pull qwen2.5:7b
```

Ensure Ollama is running at `http://localhost:11434`, then:

```powershell
mvn clean spring-boot:run
```

## Test endpoint

curl.exe -X POST "http://localhost:8080/api/support/ORD-101" -H "Content-Type: application/json" -d "{\"question\":\"Payment was deducted but the order was not created. Investigate the issue.\"}"

Expected demo tool results:
- Payment: `SUCCESS`
- Order: `NOT_CREATED`

Tool selection and ordering are model decisions; the model may choose either order or may not call both tools on every run.

## Test invalid input

```powershell
curl.exe -X POST "http://localhost:8080/api/support/INVALID" `
  -H "Content-Type: application/json" `
  -d '{\"question\":\"Check payment status.\"}'
```

The URL's order ID is passed in the prompt, but the policy validates the arguments the model actually sends to a tool. This endpoint example does not independently enforce URL-level authorization.

## How the guard works

`AiConfig` exposes only guarded callbacks to the ChatClient. `GuardedToolCallback.call(...)` invokes `ToolExecutionPolicy.validate(...)` before delegating to the underlying callback. If validation throws, delegation does not occur.

## Important security notes

- This is an educational demonstration, not a complete authorization solution.
- Validate the authenticated user's access to the order against your real database.
- Consider allowlists, rate limits, audit logs, argument-size limits, and controlled error handling.
- Avoid exposing unguarded callbacks for the same tools through another execution path.
- Test the exact API signatures against the Spring AI version you use.
