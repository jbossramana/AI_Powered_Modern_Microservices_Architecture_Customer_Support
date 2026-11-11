# Customer Service: REST API + Spring AI Tool Calling

A minimal teaching example targeting **Spring Boot 4.1.0**, **Spring AI 2.0.0**, and **Ollama `llama3.2`**.

## What this demonstrates

The same `CustomerService.getCustomerById(id)` business method is reused in two paths:

1. `GET /api/customers/{id}` calls the service directly. No LLM is involved.
2. `POST /api/ai/ask` sends a natural-language question to the LLM and registers `CustomerTools` as a tool. If the model chooses the customer lookup tool, Spring AI executes it; the tool delegates to the same `CustomerService`.

`CustomerTools` is an adapter, not a second implementation of the business rules.

## Requirements

- JDK 21
- Maven 3.9+
- Ollama installed and running
- The `llama3.2` model pulled locally

## 1. Start Ollama

In Windows Command Prompt:

```bat
ollama pull llama3.2
ollama serve
```

If Ollama is already running as a background service, you may not need to run `ollama serve` again. Confirm the model is available:

```bat
ollama list
```

## 2. Run the Spring Boot application

Open Command Prompt in the project folder (the folder containing `pom.xml`):

```bat
mvnw.cmd spring-boot:run
```

If the Maven wrapper is not included in your environment, use:

```bat
mvn spring-boot:run
```

The application listens on `http://localhost:8080`.

## 3. Test path 1: REST API, without an LLM

```bat
curl.exe -i "http://localhost:8080/api/customers/101"
```

Expected response body:

```json
{"id":101,"name":"Ravi","city":"Hyderabad","membership":"GOLD"}
```

This path is:

`HTTP GET -> CustomerRestController -> CustomerService -> JSON`

## 4. Test path 2: Natural-language question, with the LLM

Use Windows Command Prompt quoting exactly as shown:

>>response: text
curl.exe -X POST "http://localhost:8080/api/ai/ask" -H "Content-Type: application/json" -d "{\"question\":\"Please find customer 101 and tell me the customer's details.\"}"

>>response:json

curl.exe -X POST "http://localhost:8080/api/ai/ask/json" -H "Content-Type: application/json" -d "{\"question\":\"Please find customer 101 and tell me the customer's details.\"}"

>> response:entity
curl.exe -X POST "http://localhost:8080/api/ai/ask/customer" -H "Content-Type: application/json" -d "{\"question\":\"Please find customer 101 and tell me the customer's details.\"}"

The model should choose the customer lookup tool, and Spring AI will call:

`CustomerTools.getCustomerById(101) -> CustomerService.getCustomerById(101)`

The model then produces a conversational response based on the returned customer data. Wording may vary.

Try another question:

```bat
curl.exe -X POST "http://localhost:8080/api/ai/ask" -H "Content-Type: application/json" -d "{\"question\":\"Who is customer 102 and what is their membership?\"}"
```

## 5. Test a missing customer

REST path:

```bat
curl.exe -i "http://localhost:8080/api/customers/999"
```

AI path:

```bat
curl.exe -X POST "http://localhost:8080/api/ai/ask" -H "Content-Type: application/json" -d "{\"question\":\"Find customer 999.\"}"
```

The REST endpoint returns HTTP 404 for a missing customer. The AI path may describe the tool error in natural language; exact wording depends on model/tool error handling.

## Configuration

`src/main/resources/application.properties`:

```properties
spring.application.name=customer-service-ai
server.port=8080
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=llama3.2
```

## Key classes

- `CustomerService`: business logic and demo data; has no Spring AI dependency.
- `CustomerRestController`: normal REST endpoint.
- `CustomerTools`: exposes the service method using `@Tool`.
- `CustomerAiController`: sends the user's natural-language question to `ChatClient` and registers `CustomerTools`.

## Important notes

Spring AI knows the target Java type from .entity(Customer.class). Depending on the configured structured-output converter and model support, it can provide format instructions or a schema to the LLM.

Important distinction: .entity(Customer.class) does not guarantee that the model always generates the correct JSON field names. The conversion may fail or produce incomplete data if the response does not match the expected structure.


