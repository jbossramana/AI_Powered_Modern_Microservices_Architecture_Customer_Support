# Spring AI 2.0.0 + Spring Boot 4.1.0 + Ollama

A teaching project demonstrating different **inputs to an LLM** and different **outputs from an LLM** using Spring AI `ChatClient`.

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring AI 2.0.0
- Ollama
- Default chat model: `llama3.2`
- Maven

> Version note: this project declares the exact versions requested. If a version is not yet available in your Maven repository, use the corresponding released version available to your environment.

## 1. Prerequisites

Install Java 21, Maven, and Ollama. In a terminal, download a text model:

```powershell
ollama pull llama3.2
```

Run Ollama and confirm the model is available:

```powershell
ollama list
```

The image endpoint requires a vision model. For example, you can pull a compatible model and run the application with that model selected:

```powershell
ollama pull llava
$env:OLLAMA_CHAT_MODEL = "llava"
```

`llama3.2` in this project is the text default; do not expect it to understand images. The model and Spring AI Ollama integration must support image input.

## 2. Start the application

From the project root:

```powershell
mvn spring-boot:run
```

Or build and run the JAR:

```powershell
mvn clean package
java -jar target\spring-ai-input-output-demo-0.0.1-SNAPSHOT.jar
```

Base URL: `http://localhost:8080`

## 3. Windows CMD / PowerShell curl commands

Use `curl.exe` on Windows. These examples use single-line JSON to avoid Windows quoting issues.

### A. Direct customer REST lookup (no LLM)

```powershell
curl.exe -i "http://localhost:8080/api/customers/101"
```

Expected data includes customer 101, Ravi, Hyderabad, GOLD. This endpoint reads the in-memory Java map directly and does not call the LLM.

### B. Input: plain text; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Explain Spring AI in three points\"}"
```

### C. Input: system message + user message; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/system" -H "Content-Type: application/json" -d "{\"message\":\"Explain dependency injection with a Java example\"}"
```

### D. Input: template variables; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/template" -H "Content-Type: application/json" -d "{\"topic\":\"Polymorphism\",\"audience\":\"beginner Java developers\"}"
```

### E. Input: Java object fields; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/customer-summary" -H "Content-Type: application/json" -d "{\"id\":101,\"name\":\"Ravi\",\"city\":\"Hyderabad\",\"membership\":\"GOLD\"}"
```

The controller receives a `Customer` JSON object. The service formats its fields into prompt text for the LLM; it does not send a Java object reference over the network.

### F. Input: chat history; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/history" -H "Content-Type: application/json" -d "{\"previousUserMessage\":\"I made a duplicate payment\",\"previousAssistantMessage\":\"I can help you check the refund policy\",\"currentQuestion\":\"What should I do next?\"}"
```

This demo supplies a short history explicitly in the request. It does not persist sessions or automatically remember previous HTTP requests.

### G. Input: retrieved documentation / context; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/context" -H "Content-Type: application/json" -d "{\"ticketText\":\"How long does a refund take?\",\"documentation\":\"Refunds are processed within five business days.\"}"
```

This demonstrates passing context to the model. It is a RAG-style prompt example, not a full vector database implementation.

### H. Input: Java tool; output: String

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/tool" -H "Content-Type: application/json" -d "{\"message\":\"Look up customer 101 and tell me the city and membership\"}"
```

Spring AI exposes the `@Tool` method on `CustomerService` to the model. If the model requests the tool, Spring AI invokes the Java method and supplies the result back to the model. Tool selection depends on the model.

### I. Output: Java Customer object

```powershell
curl.exe -X POST "http://localhost:8080/api/output/customer" -H "Content-Type: application/json" -d "{\"message\":\"Create a sample customer with id 201, name Meera, city Pune, membership SILVER\"}"
```

Spring AI maps the model's structured response into a `Customer`. This example asks the model to generate sample data; do not use model-generated records as authoritative production customer data.

### J. Output: List<Customer>

```powershell
curl.exe -X POST "http://localhost:8080/api/output/customers" -H "Content-Type: application/json" -d "{\"message\":\"Return two sample customers: id 201 name Meera city Pune membership SILVER; id 202 name Arjun city Delhi membership GOLD\"}"
```

### K. Output: Map<String,Object>

```powershell
curl.exe -X POST "http://localhost:8080/api/output/map" -H "Content-Type: application/json" -d "{\"message\":\"Classify this ticket: Customers cannot log in after production deployment. Return category, priority, sentiment, and recommendedAction as a complete structured object. category, priority, sentiment, and recommendedAction must all have values.\"}"
```

### L. Output: ChatResponse

```powershell
curl.exe -X POST "http://localhost:8080/api/output/chat-response" -H "Content-Type: application/json" -d "{\"message\":\"Explain microservices briefly\"}"
```

This endpoint returns the Spring AI `ChatResponse`, which contains generated content and available metadata. The exact JSON structure depends on the Spring AI version and model provider.

### M. Input: image + text (vision model required)

Create `C:\images\error.png`, then run this in PowerShell:

```powershell
curl.exe -X POST "http://localhost:8080/api/chat/image" -F "image=@C:\images\error.png;type=image/png" -F "question=Describe the error shown in this image"
```

Use a vision-capable Ollama model, for example `llava`, by setting `OLLAMA_CHAT_MODEL` before starting the application. A text-only model cannot interpret the image.

## 4. Configuration

`src/main/resources/application.properties`:

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=${OLLAMA_CHAT_MODEL:llama3.2}
spring.ai.ollama.chat.options.temperature=0.2
```

Change the model with a PowerShell environment variable before starting the app:

```powershell
$env:OLLAMA_CHAT_MODEL = "llava"
mvn spring-boot:run
```

For normal text endpoints, use a text model. For the image endpoint, use a compatible vision model.

## 5. Concepts file

Read [`CONCEPTS.md`](CONCEPTS.md) for detailed explanations of every input and output type, the request flow, tool calling, structured output, limitations, and troubleshooting.

## 6. Important learning notes

- `.content()` returns text as a Java `String`.
- `.entity(Customer.class)` maps model output to a Java record/class; validate it before business use.
- `ParameterizedTypeReference<List<Customer>>` retains generic type information.
- `.chatResponse()` returns a full response object with content and metadata.
- A Java object is usually represented as text or structured content in the model request; the LLM does not receive a JVM object reference.
- Tool calls are different from ordinary prompt context: the model requests a tool, and Spring AI executes the Java method.
- A RAG prompt requires relevant context to be retrieved by an application component. This demo passes context in the request; it does not implement a vector store.
- Chat history in this demo is supplied by the caller; it is not persisted automatically.
- The model can produce inaccurate output. Validate structured data and never treat generated data as a trusted database record.
