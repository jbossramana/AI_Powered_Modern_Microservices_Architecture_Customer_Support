# Agentic Tool-Calling Loop: Concept and Actual Java Implementation

This project now contains a **manual Java `while` loop** for the multi-tool agentic example. It uses Spring AI's `ChatModel` and `ToolCallingManager`, instead of delegating the whole loop to `ChatClient`'s automatically registered tool-calling advisor.

## The conceptual loop

```java
response = callModel(prompt);
while (modelRequestedTool(response) && withinLimits()) {
    result = executeAuthorizedJavaTool(response.toolCall());
    addToolResultToConversation(prompt, result);
    response = callModel(prompt);
}
return finalAnswer(response);
```

## Where it is implemented

Open `src/main/java/com/example/support/service/WorkflowService.java` and find `agenticMultipleTools(...)`.

The core loop is:

```java
while (response != null && response.hasToolCalls() && iterations < maxIterations) {
    iterations++;
    ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);

    if (result.returnDirect()) {
        // Return the last tool result directly, without another model call.
        ...
    }

    prompt = new Prompt(result.conversationHistory(), options);
    response = chatModel.call(prompt);
}
```

### What each part does

1. `ChatModel.call(prompt)` sends the current conversation to the LLM and returns its response.
2. `response.hasToolCalls()` checks whether the LLM requested one or more tools.
3. `ToolCallingManager.executeToolCalls(prompt, response)` resolves and executes the registered Java tool callbacks.
4. `result.conversationHistory()` includes the conversation, the assistant's tool-call request, and the tool result messages.
5. Java builds the next `Prompt` with that history and calls the model again.
6. The loop stops when the model no longer requests tools. `maxIterations` bounds the number of tool-call rounds.

## Test from Windows CMD

Start Ollama and the Spring Boot application, then run:

```cmd
curl.exe -X POST "http://localhost:8080/demo/agentic/multiple-tools" -H "Content-Type: application/json" -d "{"ticketId":"TCK-1001","customerId":"C-1001","goal":"Investigate a possible duplicate payment and explain the safe next step."}"
```

The response includes `iterations`, `loopLog`, `executionFlow`, and `loopManagedBy` so learners can observe that the application owns the loop. The exact tools requested depend on the model's decisions and tool results; it is not guaranteed to call every available tool.

## Agent vs. Agentic AI

- **Agent:** an LLM-enabled component that can choose and use tools.
- **Agentic AI:** emphasizes pursuing a broader goal through planning, actions, observations, adaptation, and stopping conditions.
- The terms overlap. A tool-calling loop is an implementation mechanism, not by itself a guarantee of sophisticated agentic behavior.

## Safety notes

- This example exposes read-only demo tools.
- The loop has a maximum of five tool-call rounds.
- The tool-calling manager also applies tool-call limits.
- In production, add authorization checks, argument validation, timeouts, audit logging, and human approval for consequential operations.
- Enforce mandatory policy in Java/backend code, not only in the system prompt.

## Spring AI note

Spring AI's `ChatClient` normally manages tool calling automatically. For teaching manual control, this endpoint uses `ChatModel` directly and invokes `ToolCallingManager` inside Java's `while` loop. This is the explicit control-flow version of the same general tool-call cycle.
