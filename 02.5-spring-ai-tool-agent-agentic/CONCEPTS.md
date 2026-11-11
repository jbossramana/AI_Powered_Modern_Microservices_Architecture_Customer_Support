# Instructor's Teaching Guide: Explain It Clearly to Students

## Start with one familiar problem

Tell students:

> A customer says: “I was charged twice. Please check my ticket and tell me what happens next.”

Our application may need ticket status, customer plan, payment information, and refund policy. We will solve the same problem in different ways. The business problem stays the same; **who chooses the next step changes**.

## Five terms students must separate

- **Service:** ordinary Java business code, such as `getCustomerPlan("C-1001")`.
- **Tool:** a service method made available for an LLM workflow to request. The underlying work still runs in Java.
- **LLM:** understands the request and generates language. It can also propose a tool call if tool calling is enabled.
- **Agent:** in this course, an LLM-enabled component that can choose from tools it has been given.
- **Agentic workflow:** a goal-driven workflow that can act, inspect results, and decide on a next step, within limits.
- **Multi-agent system:** two or more specialized agents coordinated to complete a task.

A useful analogy: **tools are the tools in a toolbox; the LLM is a decision-making assistant; Java is the trusted operator that actually runs the tool and enforces permissions.** This analogy is not exact, but it helps students remember the roles.

## Teach the examples in this order

### Example 1 — Java only, one service

Teacher says: “The question is already clear. Java knows exactly what to do.”

```text
Request -> Java -> getCustomerPlan() -> result
```

No LLM and no agent are required. Use this for a simple lookup.

### Example 2 — Java only, multiple services

Teacher says: “Java knows the complete workflow and its order.”

```text
Java -> getTicketStatus()
     -> getCustomerPlan()
     -> getSupportPolicy()
     -> combine results
```

Still no LLM and no agent. This is normal application orchestration.

### Example 3 — Java calls one service, then uses an LLM

Teacher says: “Java retrieves the fact; the LLM explains the fact.”

```text
Java -> getCustomerPlan() -> verified plan: Pro
                              |
                              v
                            LLM -> plain-language explanation
```

This uses an LLM, but **it is not an agent**, because the LLM did not choose or call the service. It only generated an explanation.

### Example 4 — Java calls multiple services, then uses an LLM

Teacher says: “Java collects all the facts first. The LLM turns them into a readable summary.”

```text
Java -> ticket status
     -> customer plan
     -> support policy
     -> verified facts -> LLM summary
```

This is also a no-agent design. The workflow is fixed in Java.

### Example 5 — Agent with one tool

Teacher says: “Now the LLM can decide whether the customer-plan tool is needed.”

```text
Question -> LLM chooses getCustomerPlan()
          -> Java executes it
          -> result returns to LLM
          -> LLM answers
```

This is an agent pattern because the model chooses a tool. Java still executes the method.

### Example 6 — Agent with multiple tools

Teacher says: “The LLM can choose between ticket, plan, and policy tools.”

```text
Question -> LLM chooses tool A -> Java runs A -> result
          -> LLM decides whether tool B is needed
          -> Java runs B -> result -> answer
```

Tool selection can vary depending on the question and earlier results. The model can make a poor choice, so Java must validate tool arguments and permissions.

### Example 7 — Agentic workflow

Teacher says: “The system is given a goal and can adjust its next step after seeing results.”

```text
Goal -> choose next step -> act using a tool
     -> observe result -> goal achieved?
                            | no: choose next step
                            | yes: stop
```

For a duplicate-payment investigation, the system may check the ticket, inspect payment details, retrieve the refund policy, and then summarize what is verified. The next step depends on the evidence.

**Important teaching nuance:** Agentic AI is not defined only by having many tools. It emphasizes goal-directed, iterative, observation-driven behavior. A one-tool lookup usually does not need an elaborate agentic loop. In real-world terminology, “agent” and “agentic AI” overlap; explain the practical distinction rather than claiming every team uses the words identically.

### Example 8 — Multi-agent system

Teacher says: “Instead of one agent doing every task, we assign specialist roles.”

```text
Orchestrator
   -> TriageAgent: classify the issue
   -> InvestigationAgent: collect verified facts using tools
   -> ResponseAgent: write the customer-facing answer
   -> final response
```

The agents are separate role-specific LLM calls or components. The orchestrator decides the handoff order and passes information between them. Multi-agent does not automatically mean better; it can add cost, latency, and coordination problems.

## Comparison students can memorize

| Design | Who chooses the service/tool? | Does it use an LLM? | What makes it different? |
|---|---|---|---|
| Java only, one service | Java | No | One direct call |
| Java only, multiple services | Java | No | Fixed Java workflow |
| Java + LLM, no agent | Java chooses services | Yes | LLM explains/summarizes facts |
| Agent + one tool | LLM chooses whether to use the tool | Yes | Model-driven tool choice |
| Agent + multiple tools | LLM chooses among tools | Yes | Flexible tool selection |
| Agentic workflow | Agent adapts next steps toward a goal | Usually yes | Plan/act/observe/continue/stop |
| Multi-agent | Orchestrator and specialist agents | Usually yes | Multiple specialist roles cooperate |

## Three common misconceptions to correct

1. **“If we use an LLM, it is an agent.”** False. An LLM can simply summarize verified data.
2. **“A tool is something the LLM runs directly.”** Not exactly. The model requests a tool call; application code validates and executes it.
3. **“More agents mean more intelligence.”** Not necessarily. Use the simplest architecture that meets the need.

## A short classroom script

You can say:

> “First, Java can call services directly. That is ordinary application code. Second, Java can collect facts and ask an LLM to explain them; this uses an LLM but not an agent. Third, if the LLM can choose which tool Java should execute, we call that an agent pattern. Fourth, if the workflow repeatedly acts, observes results, and changes its next step to achieve a goal, we call it agentic. Finally, if multiple specialized agents hand work to one another under an orchestrator, it is a multi-agent system. In every case, Java and trusted backend services remain responsible for authorization, data access, and mandatory business rules.”

## Quick check questions for students

1. Java always calls `getCustomerPlan()` and the LLM explains the result. Is it an agent? **No.**
2. The LLM chooses between ticket-status and plan tools. Is it an agent pattern? **Yes.**
3. The system checks payment details, observes the result, then decides whether to retrieve refund policy. What behavior is being demonstrated? **An agentic workflow.**
4. Triage, investigation, and response are handled by separate specialist agents. What is this? **A multi-agent system.**
5. Who should enforce the rule that a refund cannot be issued without authorization? **The trusted Java/backend policy layer, not only the LLM prompt.**

---

# Concepts: Tools, Agents, Agentic AI, and Multi-Agent Systems

## 1. Tool

A **tool** is a callable capability exposed to a model-driven workflow. In this project, `SupportTools` contains methods such as:
- `getTicketStatus(ticketId)`
- `getCustomerPlan(customerId)`
- `getSupportPolicy(plan, category)`
- `getOrderDetails(customerId, ticketId)`
- `getPaymentDetails(customerId, ticketId)`

The model does not query the database directly. Spring AI sends tool descriptions to the model; the model may request a tool call; application code executes the Java method and returns the result.

## 2. Without an agent

### Single tool
`WorkflowService.noAgentSingleTool()` directly calls `SupportDataService.getCustomerPlan()`.

**Intent:** use normal Java when the operation is already known. It is simple, predictable, cheap, and easy to test.

### Multiple tools
`WorkflowService.noAgentMultipleTools()` calls ticket status, customer plan, and support policy in a defined sequence.

**Intent:** deterministic business workflows. Java owns the branching, ordering, error handling, and policy enforcement.

```text
HTTP -> Java workflow -> tool/service A -> tool/service B -> combine -> response
```


## 2B. No agent, but still using an LLM

**No agent does not mean no LLM.** Java can call services itself and then use an LLM only to explain or summarize the verified results. The model is not given tools and cannot choose or execute them.

### Single service + LLM explanation
1. Java calls `getCustomerPlan("C-1001")`.
2. Backend returns the verified value `Pro`.
3. Java sends that fact to the LLM.
4. LLM explains the result in plain language.

### Multiple services + LLM summary
1. Java calls ticket status.
2. Java calls customer plan.
3. Java calls support policy.
4. Java passes those results to the LLM.
5. LLM produces a readable summary based only on those facts.

```text
No-agent + LLM:
Java decides and calls service(s) -> verified data -> LLM writes/explains -> response

Agent with tools:
Question -> LLM chooses tool -> Java executes tool -> result returns to LLM -> answer
```

The difference is **who chooses the tools**. In a no-agent workflow, Java chooses the services and sequence. The LLM can still provide language understanding, summarization, classification, or explanation after the facts are gathered.

## No agent, but still using an LLM

**No agent does not mean no LLM.** Java can choose and call its services itself, then use an LLM to explain or summarize verified results. The LLM is not given tools in these examples, so it cannot choose or execute them.

### Single service + LLM explanation
1. Java calls `getCustomerPlan("C-1001")`.
2. The backend returns the verified plan, `Pro`.
3. Java sends that verified fact to the LLM.
4. The LLM explains the result in plain language.

Endpoint: `POST /demo/no-agent/single-tool-with-llm`

### Multiple services + LLM summary
1. Java calls ticket status.
2. Java calls customer plan.
3. Java calls support policy.
4. Java passes those verified facts to the LLM.
5. The LLM writes a concise summary based on the supplied facts.

Endpoint: `POST /demo/no-agent/multiple-tools-with-llm`

```text
No-agent + LLM:
Java decides and calls service(s) -> verified facts -> LLM explains/summarizes -> response

Agent with tools:
Question -> LLM chooses a tool -> Java executes tool -> result returns to LLM -> answer
```

The key difference is **who selects the services/tools**. In a no-agent workflow, Java selects them and controls the sequence. The LLM can still be used for explanation, summarization, classification, or natural-language generation.

## 3. Agent with one tool

`agentSingleTool()` gives the LLM access to only `getCustomerPlan`.

```text
Question -> LLM decides whether to call tool
         -> Java executes requested tool
         -> tool result returns to LLM
         -> natural-language answer
```

**Intent:** let a natural-language request decide whether the lookup is relevant. If Java already knows the lookup must happen, direct Java may be simpler.

## 4. Agent with multiple tools

`agentMultipleTools()` exposes several read tools. The LLM chooses which tool or tools are useful.

**Intent:** flexible selection for requests that vary. The model may need ticket status for one question, plan and policy for another, or payment details for a billing issue.

**Risk:** model decisions are not guaranteed. Validate arguments, limit available tools, and enforce mandatory rules in Java.

## 5. Agentic AI with one tool

`agenticSingleTool()` gives a goal and one available tool. The agent can decide whether the status lookup satisfies the goal.

```text
Goal -> decide -> call tool -> observe result -> goal satisfied? -> stop
```

**Intent:** illustrate the act/observe/decide pattern. A single lookup may not justify a complex loop; choose the simplest workflow that meets the requirement.

## 6. Agentic AI with multiple tools

`agenticMultipleTools()` gives a goal and multiple read-only tools. The model can adapt the next tool call to earlier observations.

Example goal: investigate a possible duplicate payment.
1. Check ticket status.
2. Check order/payment details if needed.
3. Retrieve policy.
4. Summarize verified facts and remaining checks.
5. Stop.

**Intent:** investigate a goal where the next action depends on what the system learns.

A tool-calling loop is not itself business logic. It is orchestration that lets the model receive tool results and choose a next step. Spring AI 2.x `ChatClient` manages this loop through its tool-calling advisor. A direct `ChatModel` call does not automatically execute tool calls.

## 7. Multi-agent

The `multiAgent()` method uses three separate role-specific LLM calls, coordinated by Java:

- **TriageAgent:** classify the ticket.
- **InvestigationAgent:** gather facts using read-only tools.
- **ResponseAgent:** draft a customer response from the triage and investigation notes.

```text
Request -> TriageAgent -> InvestigationAgent + tools -> ResponseAgent -> result
              \________________ Java orchestrator _________________/
```

This is a simple sequential multi-agent demonstration. It is not a fully autonomous supervisor graph. Java defines the order and passes each output to the next role.

**Intent:** split work into specialist responsibilities. Multi-agent adds latency, cost, and coordination complexity, so only use it when the separation is useful.

## 8. Differences summary

| Feature | No agent | Agent | Agentic workflow | Multi-agent |
|---|---|---|---|---|
| Who selects next action? | Java | Usually LLM selects tool | Agent adapts over goal-directed steps | Orchestrator and specialist agents |
| Tools | Java calls methods | LLM may request tools | Tool calls form iterative workflow | Each agent may have its own tools |
| Workflow | Fixed | Flexible tool choice | Iterative, observation-driven | Handoffs among roles |
| Best for | Known business process | Natural-language tool selection | Multi-step goal | Distinct specialist tasks |
| Main trade-off | Less flexible | Model may choose poorly | Needs limits and stopping criteria | Higher cost and coordination |

Terminology differs between libraries and teams. In practice, an agent using several tools in a loop may also be called agentic AI.

## 9. Production safeguards

1. The LLM proposes; Java validates and executes.
2. Keep database/services as the source of truth.
3. Keep mandatory policy in deterministic Java logic.
4. Treat ticket text as untrusted input.
5. Separate read-only tools from side-effecting tools.
6. Require authorization and explicit approval for refunds or cancellations.
7. Add timeouts, max tool calls, and stopping conditions.
8. Validate tool arguments and model output.
9. Log outcomes without exposing secrets or unnecessary personal data.
10. Never claim an action completed unless a trusted backend confirms it.


## How the agentic tool-calling loop works in this project

The endpoint `POST /demo/agentic/multiple-tools` is the practical example. In `WorkflowService.agenticMultipleTools()`, the important call is:

```java
String answer = chatClient.prompt()
        .system("...")
        .user("Customer ID: " + customerId + "\nTicket ID: " + ticketId + "\nGoal: " + goal)
        .tools(tools)
        .call()
        .content();
```

The exact prompt text is defined in the Java class. The key point is `.tools(tools).call()`: Spring AI manages the tool-call cycle. The conceptual sequence is:

```text
Prompt + goal + tool descriptions
          |
          v
       LLM call
          |
          v
   Tool call requested? ---- no ----> Return final answer
          | yes
          v
Spring AI executes the Java @Tool method
          |
          v
Tool result is sent back to the LLM
          |
          v
       LLM call again
          |
          +---- may request another tool, or return final answer
```

This corresponds to the earlier conceptual pseudocode:

| Conceptual pseudocode | In this project |
|---|---|
| `ollamaModel.call(prompt)` | Spring AI sends the prompt/model request as part of `chatClient...call()` |
| `response.hasToolCalls()` | Spring AI interprets the model's tool-call response internally |
| `executeJavaTool(...)` | Spring AI invokes the registered Java method from `SupportTools` |
| Add tool output to conversation | Spring AI returns the tool result to the model as part of its managed tool cycle |
| Call the model again | Spring AI continues the cycle when more tool calls are requested |
| Return final text | `.content()` returns the final response text |

**Important:** the pseudocode is a teaching model, not a drop-in implementation. Methods such as `hasToolCalls()`, `getToolCalls()`, and `addMessage()` in that snippet are illustrative names. For this demo, do not implement a second manual loop on top of Spring AI's tool handling.

### Why this demonstrates agentic behavior

The customer gives a goal, not a fixed sequence of service calls. The LLM can inspect one tool result and choose another read-only tool if needed. This is goal-oriented, iterative behavior. An agent can also call multiple tools; the distinction is the workflow's emphasis on pursuing a goal, observing evidence, adapting the next step, and stopping safely—not simply the number of tool calls.

### Production safeguards

Set practical limits for execution time and tool calls, validate tool arguments, enforce authorization and business rules in Java, and require human approval for consequential actions. A system message alone is not a security boundary.
