# Spring AI input and output concepts — detailed guide

This guide accompanies the Spring Boot 4.1.0 / Spring AI 2.0.0 / Ollama demo.

## 1. The basic request flow

1. A client sends an HTTP request to a Spring MVC controller.
2. The controller validates/deserializes the incoming JSON.
3. Your service builds a `ChatClient` request.
4. Spring AI converts the prompt and any media/tool definitions into the model-provider request.
5. Ollama runs the selected model and returns content and/or a tool-call request.
6. Spring AI handles the response. If a tool call is requested, Spring AI invokes the registered Java method and can send its result back to the model.
7. Your code chooses how to expose the result: text, a Java object, a list, a map, or a full `ChatResponse`.

The LLM never receives a Java heap reference. Inputs must be represented in a form the model API accepts, such as text, messages, media, or tool definitions/results.

## 2. Input types

### A. Plain text (`String`)

Example endpoint: `POST /api/chat`

Request:
```json
{"message":"Explain Spring AI in three points"}
```

Code shape:
```java
chatClient.prompt()
    .user(question)
    .call()
    .content();
```

Use this for questions, instructions, summaries, and text generation. `.content()` extracts the generated text.

### B. System and user messages

Example endpoint: `POST /api/chat/system`

- The system message sets the assistant's behavior, role, constraints, and style.
- The user message contains the task or question.

```java
chatClient.prompt()
    .system("You are a concise Java trainer.")
    .user("Explain dependency injection.")
    .call()
    .content();
```

A system message guides the model; it is not a hard security boundary. Validate outputs and enforce authorization in Java.

### C. Java object input

Example endpoint: `POST /api/chat/customer-summary`

The HTTP JSON is deserialized into a `Customer` record. The application then formats the fields into prompt text. This is a deliberate mapping from Java fields to model-readable content.

```java
String context = """
    ID: %d
    Name: %s
    City: %s
    Membership: %s
    """.formatted(c.id(), c.name(), c.city(), c.membership());
```

This is not the same as passing a live object reference to the LLM. If the object contains private or unnecessary fields, do not include them in the prompt.

### D. Prompt template variables

Example endpoint: `POST /api/chat/template`

```java
chatClient.prompt()
    .user(u -> u.text("Explain {topic} for {audience}")
        .param("topic", topic)
        .param("audience", audience))
    .call()
    .content();
```

Templates avoid string concatenation and keep prompt text separate from variable values. Use parameter names that match placeholders exactly. Do not assume user-supplied content is trustworthy.

### E. Conversation history

Example endpoint: `POST /api/chat/history`

The caller explicitly supplies previous user and assistant messages. The application creates `UserMessage` and `AssistantMessage` instances and passes them to `.messages(...)`.

History helps preserve context across turns, but the model may still misunderstand it. This demo is stateless: no history is stored between requests. A production application should associate history with an authenticated conversation/session and apply retention and privacy rules.

### F. Retrieved documents / RAG context

Example endpoint: `POST /api/chat/context`

The request includes a documentation string. The application places it in the prompt alongside the question. This demonstrates the *context injection* portion of Retrieval-Augmented Generation (RAG).

A full RAG system additionally needs a document ingestion pipeline, chunking, embeddings, a vector store/search layer, retrieval, and often citations/source tracking. This project does not pretend to perform retrieval; the caller supplies the context.

### G. Image + text

Example endpoint: `POST /api/chat/image`

The controller accepts a multipart PNG and a text question. Spring AI attaches the image as media with the prompt. The chosen model must support vision and the installed Ollama version/model must support the required image request format.

The default `llama3.2` configuration is a text model in this example. Choose a vision-capable model (for example, `llava`) for image analysis. The `IMAGE_PNG` MIME type matches the documented PNG upload. If accepting JPEGs too, validate the content type and select the correct MIME type.

### H. Java tools

Example endpoint: `POST /api/chat/tool`

The `CustomerService.getCustomerById(...)` method is annotated with `@Tool`. Registering `.tools(customerService)` makes its tool metadata available to the model.

Typical flow:
1. The model reads the user's question and available tool definitions.
2. If appropriate, the model requests the customer lookup tool and supplies an ID argument.
3. Spring AI maps the argument to the Java parameter and invokes the method.
4. The Java method returns a `Customer` result.
5. Spring AI makes the tool result available to the model for its final answer.

The tool result may be represented as structured/serialized data in the provider conversation. The LLM does not receive the original Java object reference. Never rely on the model for authorization; enforce access control in the tool method. This demo uses a small in-memory map.

## 3. Output types

### A. `String` via `.content()`

```java
String answer = chatClient.prompt()
    .user("Explain Spring Boot")
    .call()
    .content();
```

Use this when the application needs only the generated text. A text answer can be incomplete or inaccurate.

### B. A Java record/class via `.entity(Customer.class)`

```java
Customer result = chatClient.prompt()
    .user("Return a sample customer ...")
    .call()
    .entity(Customer.class);
```

Spring AI converts structured model output into the requested type. The model is not literally returning a Java object over the network. Field names, types, required values, and model support matter. Validate the result before business use.

### C. Generic list via `ParameterizedTypeReference`

```java
List<Customer> customers = chatClient.prompt()
    .user("Return two sample customers ...")
    .call()
    .entity(new ParameterizedTypeReference<List<Customer>>() {});
```

The anonymous type reference preserves generic type information. Without it, Java's type erasure makes a raw `List.class` insufficient to fully describe `List<Customer>`.

### D. `Map<String,Object>`

```java
Map<String,Object> result = chatClient.prompt()
    .user("Classify the ticket into category, priority, sentiment, recommendedAction.")
    .call()
    .entity(new ParameterizedTypeReference<Map<String,Object>>() {});
```

Maps are useful when the response schema is flexible. A dedicated DTO/record is usually better for stable production contracts because it documents the expected fields and types.

### E. `ChatResponse`

```java
ChatResponse response = chatClient.prompt()
    .user("Explain microservices")
    .call()
    .chatResponse();
```

Use this when you need the response object, including generated content and provider/model metadata that is available. Metadata availability varies by provider/model.

### F. Streaming (`Flux<String>`)

A streaming endpoint can use:
```java
return chatClient.prompt()
    .user(question)
    .stream()
    .content();
```

This returns pieces of generated text as they arrive rather than waiting for one complete string. It is not implemented as a separate endpoint in this starter project, but the pattern is included here for learning.

## 4. Inputs and outputs are independent choices

Inputs define what the model receives: text, messages, context, media, or tool definitions. Outputs define how the application consumes the model response: text, a mapped object, a collection, or response metadata.

For example:
- Java object input → String output.
- Text input → `Customer` output.
- History + document context → String output.
- Tool definition + question → String output after a tool interaction.
- Image + question → String output, when a vision model is selected.

## 5. Tool result vs final answer

A tool result and an LLM's final response are different stages. A Java tool may return a `Customer`; Spring AI supplies the tool result to the model. The model then writes a natural-language answer. `.entity(Customer.class)` at the end of a separate `ChatClient` call maps the *final model response* to a `Customer`; it is not how the tool method returns its Java object.

## 6. Windows testing tips

- Use `curl.exe` rather than relying on the PowerShell alias named `curl`.
- Escape double quotes inside JSON with `\"` in PowerShell/CMD examples.
- Use `-F "image=@C:\\images\\error.png;type=image/png"` for multipart image uploads; replace the path with a real file.
- If the application cannot connect, confirm Ollama is running at `http://localhost:11434`.
- Run `ollama list` and ensure the configured model is present.
- If the model returns poor structured data, try a model that follows structured output well and validate the result.

## 7. Troubleshooting

### Connection refused to Ollama
Start Ollama and check the base URL. The default is `http://localhost:11434`.

### Model not found
Run `ollama pull llama3.2` or select an installed model using `OLLAMA_CHAT_MODEL`.

### Image endpoint cannot interpret an image
Use a vision-capable model and a matching MIME type. A text-only model cannot understand image pixels.

### Tool is not called
Tool use is a model decision and depends on tool-calling support. Ask a clear question such as “Look up customer 101 and tell me the city.” Confirm the `@Tool` method is registered with `.tools(customerService)`.

### Structured output mapping fails
Use an explicit prompt with exact fields, select a capable model, inspect the raw model response if needed, and validate mapped values. Never assume that a successful Java conversion proves the data is true.

## 8. Scope and production notes

This is a learning sample, not a production-ready support platform. It uses in-memory customer data, does not implement a vector store, does not persist chat history, and does not provide authentication or authorization. Add these concerns before deploying it.
