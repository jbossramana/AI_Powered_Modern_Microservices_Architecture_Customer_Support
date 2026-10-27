# Module 6 Notes — Model Context Protocol (MCP) with Spring AI

## 1. What is MCP?

The Model Context Protocol is an open protocol for connecting AI applications to external capabilities and context through a consistent client/server contract. Instead of embedding every integration directly into each AI application, a system exposes capabilities through an MCP server. Compatible clients can discover those capabilities and invoke them using the protocol.

Think of MCP as a standardized integration boundary for AI applications. It is not itself an LLM, an agent framework, a vector database, or a replacement for business APIs. It standardizes how a client discovers and invokes tools (and, depending on server capabilities, other MCP primitives such as resources and prompts).

## 2. Why enterprises use MCP

Enterprise information is distributed across CRM, ticketing, billing, identity, inventory, document, and observability systems. Without a shared protocol, every AI application may need a bespoke integration for every system. MCP allows a system team to publish a stable capability contract once and multiple authorized AI clients to consume it.

Typical benefits:

- **Separation of concerns:** the server owns access to its system; the AI client owns conversation and orchestration.
- **Discoverability:** clients can request the available tool definitions instead of hard-coding every tool schema.
- **Interoperability:** multiple MCP-compatible clients can reuse the same server.
- **Independent deployment:** server-side capabilities can evolve separately, subject to backward compatibility.
- **Governance point:** authentication, authorization, validation, logging, and policy checks can be centralized at the server boundary.
- **Reduced integration duplication:** common data access can be reused by chat apps, internal agents, and developer tools.

MCP does not automatically make an integration secure or interoperable in every environment. Both sides must support compatible protocol versions/transports, and production access controls remain the application owner's responsibility.

## 3. Architecture in this example

```text
User
  |
  | POST /api/chat
  v
mcp-client :8080
  ChatController -> ChatClient + Ollama model
                       |
                       | model selects discovered tool callback
                       v
              Spring AI MCP client
                       |
                       | MCP over HTTP/SSE
                       v
mcp-server :8081
  MCP tool registry -> EnterpriseTools -> demo customer/ticket/SLA maps
```

The client does not import `EnterpriseTools` or call its Java methods directly. It receives remote tool callbacks from Spring AI MCP client auto-configuration. The server registers annotated methods using `MethodToolCallbackProvider`. This process boundary is the central learning objective.

## 4. MCP server

The `mcp-server` application is a capability provider and does not need a language model. Its responsibilities are to:

1. Implement business operations using Java services or adapters to real systems.
2. Describe each operation with a clear tool name and description.
3. Define input parameters that Spring AI can turn into a tool schema.
4. Register tools through a `ToolCallbackProvider` bean.
5. Receive protocol requests and return structured tool results.
6. Apply validation, authorization, auditing and data minimization before accessing sensitive systems.

### Tool annotations

`@Tool` provides a tool description that helps the model decide when a capability is appropriate. `@ToolParam` describes parameters and contributes to the schema exposed to clients. Descriptions should be explicit about what the tool does, the identifier format, limitations and when to use it. Vague descriptions lead to poor tool selection.

### Tools in this example

| Tool | Purpose | Example input |
|---|---|---|
| `getCustomer` | Retrieve a customer account record | `C-1002` |
| `getTicketHistory` | List tickets for a customer | `C-1002` |
| `getTicketById` | Retrieve one ticket, including status and priority | `TCK-1003` |
| `getPlanSla` | Return a plan's uptime/response/breach policy | `Business` |
| `getCustomerSupportSnapshot` | Combine customer, tickets and SLA into one response | `C-1002` |

The sample uses in-memory maps for deterministic learning. A real implementation would replace these maps with repository/service adapters and apply access rules for the authenticated user and tenant.

## 5. MCP client

The `mcp-client` application exposes a regular REST endpoint (`POST /api/chat`) to its caller. Under the hood:

1. Spring AI MCP client configuration connects to the named MCP server.
2. MCP initialization negotiates protocol capabilities/version.
3. The client discovers available tools and their schemas.
4. Spring AI adapts remote MCP tools into `ToolCallback` instances.
5. The callbacks are supplied to the `ChatClient`.
6. The model decides whether to call a tool based on the user request and tool descriptions.
7. Spring AI sends the invocation to the MCP server; the tool result is returned to the model.
8. The model creates a user-facing response grounded in the returned data.

The model proposes tool calls; the application framework executes them. Do not assume the model can access data merely because a tool exists: the tool must be connected, authorized, selected and successfully executed.

## 6. SSE and MCP communication

Server-Sent Events (SSE) is an HTTP-based mechanism for streaming server-to-client events over a long-lived connection. In the configured Spring AI setup, the server's SSE transport is exposed at `/sse` and the client uses the server base URL. MCP messages carry protocol operations such as initialization, tool discovery and tool invocation; they are not ordinary REST calls designed for direct manual browsing.

Transport choices depend on Spring AI and MCP SDK versions. Streamable HTTP is also supported by current MCP implementations and may be preferable for new deployments. This example deliberately retains the SSE configuration already present in the supplied project to demonstrate the requested SSE/MCP communication. For production, choose a transport supported by both endpoints and the deployment platform, and verify the exact endpoint paths against the Spring AI version being used.

## 7. Remote MCP tools vs local in-process tools

| Concern | Local tool calling | MCP remote tool calling |
|---|---|---|
| Location | Same application/JVM | Separate server/process, possibly another host |
| Invocation | Direct Java callback | Protocol request over a transport |
| Discovery | Often known at compile/configuration time | Can be discovered from the MCP server |
| Deployment | Client and tool often released together | Server can be deployed independently |
| Latency | Usually lower | Network and protocol overhead |
| Failure modes | Exceptions and local resource failures | Network timeout, unavailable server, protocol/auth errors too |
| Security boundary | Application process boundary | Explicit remote boundary; identity and authorization must be designed |
| Reuse | Often tied to one app/module | Multiple compatible clients can reuse the server |
| Best fit | Simple, private capabilities in one service | Shared enterprise integrations and independently owned capabilities |

MCP is not automatically better for every tool. Local tools are simple and fast. Use MCP when the separation, discoverability, reuse or ownership boundary is valuable enough to justify the network hop and operational complexity.

## 8. Enterprise use cases

- Customer support: customer profile, ticket status, plan/SLA details.
- IT operations: incident, deployment, monitoring and runbook lookup.
- Sales and CRM: account summaries, opportunity status, approved customer information.
- Finance operations: read-only invoice and payment lookups with strict authorization.
- HR operations: policy and leave balance lookup, with identity-aware access control.
- Developer productivity: repository search, build status and issue tracker access.
- Supply chain: order status, shipment tracking and inventory lookup.

A tool should expose a narrow business capability rather than an unrestricted database or shell. Prefer `getTicketById(ticketId)` to a generic tool that executes arbitrary SQL.

## 9. Security and governance checklist

The example adds identifier normalization, not-found responses, required chat-input validation and invocation logs. These are educational foundations, not a complete security implementation.

Before production:

- Authenticate MCP clients (for example, an appropriate OAuth/OIDC or service identity design).
- Authorize every tool call and every record access; do not trust the model's intent.
- Enforce tenant and user-level row filtering at the server, not in prompts.
- Use least privilege; separate read tools from state-changing tools.
- Validate and bound every argument; cap result sizes and execution time.
- Avoid exposing secrets or unnecessary PII in tool output and logs.
- Add rate limiting, concurrency limits, timeout/retry policy and circuit breaking where appropriate.
- Audit actor, tool name, target identifier, outcome and correlation ID without logging secrets.
- Treat tool output and retrieved content as untrusted data; defend against prompt injection in returned text.
- Require explicit human approval for high-impact writes, financial actions or irreversible operations.
- Store credentials in a secret manager/environment injection, not source control.
- Add metrics, tracing, health checks and alerts for latency, failures and tool usage.
- Version tool contracts carefully; preserve compatibility or coordinate breaking changes.

## 10. Observability

The server logs tool invocation names and the relevant normalized identifier. In a production service, use structured logs and correlation/trace IDs so a request can be followed from the REST API through the model, MCP client, MCP server and downstream system. Track tool invocation counts, latency, error rate, timeouts and denied requests. Keep prompts and tool results out of logs by default when they may contain customer information.

## 11. Reliability and failure handling

Remote calls introduce failure modes absent from local calls: server unavailable, handshake/version mismatch, transport interruption, timeout and malformed responses. Configure sensible request timeouts; return safe, actionable errors; use retries only for operations where retrying is safe; and use circuit breakers or bulkheads when needed. A retry on a read is usually safer than blindly retrying a write. Do not let the model invent an answer when a tool failed—make tool failures visible to the orchestration layer and response policy.

## 12. Grounding and hallucination control

The system prompt instructs the assistant to use tools for real customer, ticket and SLA data and not guess account details. This helps but is not a security boundary. Stronger production designs should:

- Require a successful tool result before stating customer-specific facts.
- Distinguish “not found” from “tool unavailable” and “access denied.”
- Include source/provenance in the answer when useful.
- Validate structured tool output before it is passed to downstream actions.
- Evaluate tool selection, argument accuracy, factuality and refusal behavior with repeatable test cases.

## 13. Important distinction: MCP tools, resources and prompts

MCP can support several server primitives:

- **Tools:** callable operations that a model/client can choose to invoke, such as `getTicketById`.
- **Resources:** addressable context/data that a client can read, depending on server/client support.
- **Prompts:** reusable prompt templates exposed by a server, depending on support.

This example implements **tools**. It does not claim to implement MCP resources or prompt templates. Those are natural extensions for a richer server.

## 14. How to test and learn

1. Start the MCP server and confirm startup logs.
2. Start Ollama and verify the selected model is installed.
3. Start the MCP client after the server is ready.
4. Call `POST http://localhost:8080/api/chat` with a question about `C-1002` and request a customer/ticket/SLA lookup.
5. Observe server logs to see which remote tools were invoked.
6. Ask for `TCK-1003` to exercise the ticket-by-ID tool.
7. Ask about `C-9999` to test unknown identifiers and grounded behavior.
8. Stop the MCP server and repeat a client request to observe remote dependency failure.
9. Change `MCP_SERVER_URL` or `OLLAMA_CHAT_MODEL` to understand configuration boundaries.

## 15. Suggested advanced extensions

- Replace maps with PostgreSQL repositories and read-only database access.
- Add an authorization policy mapping user roles to allowed MCP tools.
- Add a separate write tool with an explicit approval workflow.
- Add resources for read-only customer or policy documents.
- Add pagination/filtering to ticket searches and bounded result schemas.
- Add integration tests that start the server and verify discovery/invocation through the client.
- Add OpenTelemetry tracing and metrics for model and MCP calls.
- Add contract tests to catch tool name/schema changes before deployment.
- Compare SSE with Streamable HTTP and an in-process `@Tool` implementation.

## 16. Key takeaways

MCP standardizes the boundary between AI clients and external capabilities. Spring AI makes it practical to publish annotated Java methods as MCP tools and consume remote tools through a `ChatClient`. The model chooses tools, but the server remains responsible for business logic, access control, validation, and data quality. For enterprise adoption, treat MCP tools as APIs: document them, secure them, monitor them, test them, and evolve their contracts carefully.
