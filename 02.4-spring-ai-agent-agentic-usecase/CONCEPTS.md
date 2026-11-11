# How the two execution styles differ

## Endpoint A: `/api/support/agent` — framework-managed tool loop

`SupportAssistantService.answerWithAgent()` calls `ChatClient` with registered tools. In Spring AI 2.x, `ChatClient` automatically registers `ToolCallingAdvisor` unless disabled. That advisor:
1. Sends the user request and tool definitions to the model.
2. Receives the model's tool-call request, if any.
3. Uses `ToolCallingManager` to execute the Java tool.
4. Sends the tool result back to the model.
5. Repeats until the model returns a response without tool calls.

The loop exists, but Spring AI owns it.

## Endpoint B: `/api/support/investigate` — explicit Java loop

`SupportAssistantService.investigateWithExplicitLoop()` disables automatic `ToolCallingAdvisor` registration for the call. The service:
1. Sends the investigation goal to the model.
2. Checks whether the response contains tool calls.
3. Executes requested tools with `ToolCallingManager`.
4. adds the tool results to conversation history and calls the model again.
5. Stops when the model has no more tool calls or the five-round safety limit is reached.

The model chooses the next tool based on evidence. Java owns the execution loop and the hard stopping limit.

## What “agentic” means here

The loop alone does not make a system meaningfully agentic. The investigation goal, model's selection of next actions, and use of earlier tool results are what make the workflow goal-directed. A fixed Java sequence such as “always call A, then B, then C” is deterministic orchestration rather than a model choosing the next step.

## Demo limitations

All `SupportTools` return simulated data. No live billing, ticketing, cloud monitoring, payment, or identity systems are contacted. The tools are read-only. This project has not been built or integration-tested in this environment; verify it with your chosen Spring AI 2.0.0 and Ollama versions before using it in a class or production.
