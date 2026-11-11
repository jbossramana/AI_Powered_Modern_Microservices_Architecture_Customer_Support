# Customer Support: Tool Calling vs. Manual-Loop Agent

**Technology stack:** Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.0 ·
Ollama

This project demonstrates three ways to build customer-support
interactions with tools:

1.  **Single-tool request** --- `POST /api/support/account`
2.  **Multi-tool investigation** --- `POST /api/support/investigate`
3.  **Manual-loop agent** --- `POST /api/support/agent/ask`, where Java
    explicitly manages tool calls and model iterations.

The demo uses in-memory sample data only.

------------------------------------------------------------------------

## 1. Prerequisites

-   JDK 21
-   Maven 3.9+
-   Ollama installed and running

### Download the model

Run in Windows Command Prompt:

``` bat
ollama pull qwen2.5:7b
```

### Start Ollama

If Ollama is not already running:

``` bat
ollama serve
```

> If Ollama is already running as a background service, do not start a
> second server.

------------------------------------------------------------------------

## 2. Start the Spring Boot application

From the project directory, run:

``` bat
mvn clean spring-boot:run
```

The application starts on port `8080`.

------------------------------------------------------------------------

## 3. Try the manual-loop agent

The manual-loop agent lets Java control the tool-execution cycle.

### Example 1: Look up a ticket and a plan's response target

``` bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"Check ticket TICK-501 and explain the Premium support response target for customer CUST-1001.\"}"
```

### Example 2: Check a ticket's status

``` bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"What is the status of ticket TICK-502?\"}"
```

### Example 3: Check a customer's plan response target

``` bat
curl.exe -X POST "http://localhost:8080/api/support/agent/ask" -H "Content-Type: application/json" -d "{\"question\":\"Check customer CUST-1002 and explain the support response target for their Basic plan.\"}"
```

------------------------------------------------------------------------

## 4. Compare the other endpoints

### Single tool: account lookup

This endpoint exposes one account-related tool.

``` bat
curl.exe -X POST "http://localhost:8080/api/support/account" -H "Content-Type: application/json" -d "{\"question\":\"Check the account status and plan for customer CUST-1001.\"}"
```

### Multiple tools: support investigation

This endpoint exposes multiple support tools, allowing the model to
select the tools needed for the question.

``` bat
curl.exe -X POST "http://localhost:8080/api/support/investigate" -H "Content-Type: application/json" -d "{\"question\":\"Check ticket TICK-501 and explain the Premium support response target.\"}"
```

------------------------------------------------------------------------

## 5. How the manual-loop agent works

The core logic is implemented in `AgenticSupportService.ask()`.

1.  Java builds a `Prompt` containing the system message, user question,
    and tool-calling options.
2.  Java calls `chatModel.call(prompt)`.
3.  Java checks `response.hasToolCalls()`.
4.  If tool calls are present, Java executes them through
    `toolCallingManager.executeToolCalls(prompt, response)`.
5.  Java obtains `result.conversationHistory()` and builds the next
    prompt.
6.  Java calls the model again with the updated conversation.
7.  The loop stops when the model returns no further tool calls or the
    iteration limit is reached.

**Safety limit:** The sample caps the loop at five iterations. Increase
this only after considering latency, cost, and safety implications.

------------------------------------------------------------------------

## 6. Logs to watch

The service logs markers similar to these:

``` text
[AGENT] Starting manual execution loop
[AGENT] Iteration 1 of 5
[AGENT] LLM requested tool call(s): [lookupTicket]
[TOOL EXECUTION] lookupTicket arguments=...
[TOOL RESULT] lookupTicket result=...
[AGENT] Iteration 2 of 5
[AGENT] Model returned final answer; loop finished.
```

The exact tools and number of iterations depend on the model's response.

Spring AI `DEBUG` logging is enabled in `application.yml`. This does
**not** guarantee that every raw HTTP request and response body is
logged. Any JSON examples in this README are illustrative.

------------------------------------------------------------------------

## 7. Automatic tool loop vs. manual tool loop

  ----------------------------------------------------------------------------
  Feature                 ChatClient tool example  Manual-loop agent
  ----------------------- ------------------------ ---------------------------
  Tool selection          LLM                      LLM

  Tool execution          Spring AI's              Java calls
                          `ToolCallingAdvisor`     `ToolCallingManager`
                          loop                     

  Continue/stop decision  Spring AI checks for     Java checks
                          additional tool calls    `response.hasToolCalls()`

  Iteration limit in this Framework                Explicitly capped at five
  sample                  behavior/configuration   iterations

  Best suited for         Typical application      Custom control, auditing,
                          requests                 approval gates, and
                                                   per-step decisions
  ----------------------------------------------------------------------------

The manual-loop example is an **application-controlled agent**. The
model chooses which tool to request, but Java owns the execution loop
and iteration limit.

It is not a fully autonomous, long-running agent: it has no durable
goals, scheduling, or persistent planning.

------------------------------------------------------------------------

## 8. Jackson note

The manual-loop service does not require an `ObjectMapper` constructor
parameter. If another part of the application uses one, ensure that the
Jackson dependency and Spring Boot auto-configuration are present.

Avoid defining multiple `ObjectMapper` beans unless there is a specific
requirement.

------------------------------------------------------------------------

## 9. Security considerations

-   The sample customer and ticket data is hard-coded and intended for
    demonstration.
-   Never treat an LLM tool call as authorization. Enforce customer
    permissions in Java services.
-   Validate tool inputs and audit side effects.
-   Do not log real customer data or raw prompts in production without
    appropriate redaction and access controls.

------------------------------------------------------------------------

## 10. References

-   [Spring AI tool
    calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
-   [Spring AI manual tool-calling
    loop](https://docs.spring.io/spring-ai/reference/2.1/api/tools/chatmodel-tool-calling.html)
