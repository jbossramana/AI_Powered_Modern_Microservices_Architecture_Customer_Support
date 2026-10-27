# Concepts: AI Guardrails, LLM Interceptors, Spring AI Advisors, and ChatClient Configuration

This document explains the concepts used in the sample application in student-friendly language.

## 1. The use case: Aria customer support

A customer asks:

> “I was charged twice for one order. What should I do?”

The application receives the question, checks it, sends an approved request to a local Ollama model, checks the model's response, and returns the approved answer.

The demo does not connect to a real ticketing, order, payment, or refund system. Therefore, it must not claim that a refund was approved or that a ticket was updated.

## 2. What is an LLM?

A Large Language Model (LLM) is an AI model trained to understand and generate language. In this project, Ollama runs the `llama3.2` model locally.

The model generates text; it does not automatically know the current state of a customer's account. Current account facts must come from trusted application services or authorized tools.

## 3. What is Spring AI ChatClient?

`ChatClient` is Spring AI's fluent API for sending prompts to a chat model and receiving responses.

In this project:

```java
String rawAnswer = chatClient.prompt()
        .user(request.question())
        .call()
        .content();
```

- `prompt()` starts a request.
- `user(...)` supplies the customer's text.
- `call()` performs a synchronous model call.
- `content()` extracts the generated text.

The `ChatClient.Builder` is injected by Spring Boot auto-configuration when a compatible chat model starter is on the classpath.

## 4. What is an Advisor?

An Advisor is a reusable component in Spring AI's advisor chain. Advisors can inspect, transform, enrich, or otherwise participate in the processing of a ChatClient request and response.

Examples of advisor use cases include:
- Logging and observability.
- Adding conversation history.
- Retrieval-Augmented Generation (RAG) context.
- Prompt/request transformation.
- Coordinating tool-calling behavior.
- Implementing custom policy checks when designed carefully.

An advisor is a mechanism, not a guarantee of safety. Its actual behavior depends on its implementation.

## 5. Advisor Configuration in ChatClient

The sample configures an advisor at client construction time:

```java
@Bean
ChatClient supportChatClient(ChatClient.Builder builder) {
    return builder
            .defaultSystem(\"\"\"You are Aria ...\"\"\")
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
}
```

### What does `defaultSystem(...)` do?

It defines baseline instructions for the assistant, such as being concise and not inventing ticket details. System instructions influence model behavior, but they are not a replacement for backend security controls.

### What does `defaultAdvisors(...)` do?

It attaches one or more advisors to the configured client by default. Calls made through this client use the configured advisor chain unless overridden by request-level configuration.

### What does `SimpleLoggerAdvisor` do?

It helps log request/response information for diagnostics. It is not an input guardrail, PII filter, authorization system, or factuality checker. Be careful with logging sensitive customer information.

### Default advisors versus request-level advisors

- `defaultAdvisors(...)`: configure advisors that apply by default to calls through the client.
- `.advisors(...)` on an individual prompt: configure or supply advisor behavior for that particular request, depending on the overload and advisor configuration.
- Advisor order matters: one advisor's changes can affect later advisors in the chain.

Use the official Spring AI documentation for exact APIs available in the chosen release.

## 6. What are AI Guardrails?

AI guardrails are policies and checks that reduce unsafe or incorrect behavior. They can be implemented at different layers of an application.

Common examples:
- Input length and format validation.
- Prompt-injection detection.
- Moderation and safety checks.
- Authorization checks before retrieving records.
- Output privacy checks.
- Verifying claims against trusted sources.
- Requiring approval for consequential actions.

A guardrail should have an explicit decision: allow, block, sanitize, or send for human review.

## 7. Before-LLM guardrails

Before-LLM checks run before the application calls the model.

In this project:

```java
inputGuardrail.validate(request.question());
```

The demo:
- Rejects empty questions.
- Limits input length.
- Blocks a few exact phrases associated with common prompt-injection attempts.

This is only a teaching example. Attackers can rephrase malicious instructions, so matching a few strings is not robust prompt-injection protection.

Other useful before-LLM checks:
- Is the caller authenticated?
- Is the caller allowed to access the requested ticket?
- Is the request within size and rate limits?
- Does the request contain prohibited content?
- Has retrieved context been filtered according to the user's permissions?

**Security rule:** enforce identity and authorization in trusted backend code. Never rely on the LLM to decide whether a user is allowed to see a record.

## 8. After-LLM guardrails

After-LLM checks run after the model generates an answer but before it is returned to the caller.

In this project:

```java
String safeAnswer = outputGuardrail.validateAndSanitize(rawAnswer);
```

The demo blocks a response if it detects an email-like pattern. This illustrates the flow, not production-grade PII detection.

Output checks can include:
- Sensitive-data detection.
- Format/schema validation.
- Policy validation.
- Checking that citations refer to retrieved documents.
- Checking factual claims against trusted system data.
- Rejecting unsupported promises such as “your refund has been processed” when no refund tool confirmed that action.

An output check cannot make every generated statement true. High-risk claims should be grounded in verified data and, where appropriate, human review.

## 9. AI Guardrails versus LLM interceptor versus Advisor

These terms describe different things.

| Term | Meaning | Example in this project |
|---|---|---|
| AI guardrail | A safety policy and enforcement check | Reject a suspicious input or a response containing an email-like value |
| LLM interceptor | A general term for code that observes or changes a model request/response in the processing path | An advisor or wrapper that checks prompts/responses |
| Spring AI Advisor | A specific Spring AI extension mechanism in the ChatClient processing chain | `SimpleLoggerAdvisor` configured with `defaultAdvisors(...)` |
| Before/after processing | The timing of a check | Input check before `call()`, output check after `content()` |
| HTTP interceptor/filter | Middleware around web requests | Authentication, correlation IDs, request logging, rate limiting |
| System prompt | Instructions sent to the model | “Do not invent ticket details” |

A guardrail is the *policy/objective*. An interceptor or advisor can be a *mechanism* to apply some of that policy. “Before” and “after” describe *when* the check runs.

## 10. Why are the guardrails outside the Advisor in this sample?

The sample deliberately keeps the simple input and output checks visible in `SupportController` so learners can clearly see the order:

1. Validate input.
2. Call `ChatClient`.
3. Validate output.
4. Return the response.

`SimpleLoggerAdvisor` is separately configured to demonstrate `ChatClient` advisor configuration.

This is a useful introductory structure because it does not imply that a logging advisor is a security control. More advanced implementations can place appropriate reusable checks inside custom Spring AI advisors, but they must implement the advisor contract for the exact Spring AI version and ensure that checks run at the correct point in the chain.

## 11. Why not use only a Spring MVC interceptor?

A Spring MVC interceptor works at the web layer, around incoming HTTP requests and controller execution. It is useful for cross-cutting web concerns.

It usually does not have a natural view of the final model response because that response is generated later inside application code. You can build a wrapper that observes the whole call, but that is a different boundary from an MVC interceptor.

Use:
- Web filter/interceptor for HTTP concerns.
- ChatClient Advisor for LLM-pipeline concerns.
- Explicit service checks for business authorization and trusted operations.
- Output validation before returning model output.

## 12. Where does RAG fit?

Retrieval-Augmented Generation (RAG) searches a trusted knowledge base and provides relevant snippets to the LLM as context.

For a refund question, a RAG system might retrieve the official refund policy. A guardrail should still:
- Enforce document access permissions.
- Treat retrieved text as reference data, not as instructions.
- Avoid answering when evidence is missing.
- Verify transactional facts through the backend rather than assuming a policy document proves a customer's payment status.

RAG is not included in this starter project; it is a next step.

## 13. Where do tools fit?

A tool is an application function the model can request through Spring AI's tool-calling mechanism. For example, a ticket-status tool could read a ticket from a backend service.

The tool implementation must authenticate/authorize and validate its inputs. Do not let the model supply arbitrary customer IDs and then trust the request without permission checks.

For consequential operations such as issuing a refund or cancelling an account, use explicit authorization, idempotency, audit logs, and possibly human approval.

## 14. Recommended enterprise flow

```text
HTTP request
   |
   v
Authentication + authorization
   |
   v
Input validation / guardrail
   |
   v
Optional RAG retrieval with access filtering
   |
   v
ChatClient + configured advisors
   |
   v
LLM (Ollama in this demo)
   |
   v
Output guardrail + evidence/policy checks
   |
   v
Return response
```

Any tool that reads or changes enterprise data must independently enforce access control. The model's text is not proof that a business action occurred.

## 15. What this demo does not provide

This project is intentionally small and does not provide:
- A production prompt-injection classifier.
- Comprehensive PII detection.
- Authentication or authorization.
- A real customer database, ticket service, or refund API.
- RAG, vector storage, or conversation memory.
- Formal output factuality verification.
- A guarantee that every malicious or sensitive response will be detected.

Use it to learn the call flow and advisor configuration. Add and test appropriate controls before production use.

## 16. Official references

- ChatClient API: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Advisors API: https://docs.spring.io/spring-ai/reference/api/advisors.html
- Ollama chat model: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- Spring AI 2.0.0 GA: https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/
