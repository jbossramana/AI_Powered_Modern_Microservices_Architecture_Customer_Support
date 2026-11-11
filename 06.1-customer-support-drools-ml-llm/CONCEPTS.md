# Concepts Covered

## Hybrid enterprise AI
- **Rules:** what must happen when a known condition is true?
- **ML:** which category or outcome is likely based on patterns learned from examples?
- **LLM:** how can a customer message be understood and answered in natural language?
- **Tools/APIs:** how can the application perform an action?
- **Spring Boot:** how are components connected into a service?

## Drools business rules
Drools evaluates conditions in `.drl` files and applies deterministic actions. Example: if category is `PRODUCTION_INCIDENT` and priority is `CRITICAL`, route to `INCIDENT_RESPONSE`, escalate, and require human approval.

Other policies could route enterprise customers, require manager approval for large refunds, or send low-confidence predictions for manual review. Rules do not learn from data. Define precedence if several rules can modify the same fields.

## Traditional ML
The Python service uses **TF-IDF** to convert text into numeric features and **Logistic Regression** to classify it. `predict_proba` returns a model score. Categories: `PRODUCTION_INCIDENT`, `ACCOUNT_ACCESS`, and `BILLING`.

The category-to-priority mapping is simplistic and for teaching only. Real systems need representative labeled data, evaluation, monitoring, and validated priority policy. A confidence score is not a guarantee of correctness.

## LLM / generative AI
The LLM drafts a customer response using the original ticket and decisions made by ML and Drools. The prompt says not to claim a fix is complete or invent an ETA. Prompting guides behavior but cannot guarantee correctness; generated text should remain reviewable.

## Spring AI and Ollama
- **Ollama** runs a model locally and exposes an HTTP API.
- **Spring AI** provides Java abstractions such as `ChatClient`.
- The app uses `spring-ai-starter-model-ollama`.
- `application.yml` configures the Ollama URL and model name.

## REST and orchestration
`POST /api/tickets/process` accepts JSON. The service calls the ML REST service, creates a Drools fact object, fires rules, calls Spring AI/Ollama, then returns a combined result. The ML model runs as a separate Python service.

## Human-in-the-loop
The example sets `humanApprovalRequired=true` for critical incidents and low-confidence predictions. This is a flag, not a full workflow. A real workflow needs persistent approval records, authorized reviewers, audit history, and a separately enforced gate before sensitive actions.

## RAG, tools, agents, MCP
These related concepts are not implemented in this sample:
- **RAG:** retrieve internal documentation and pass relevant passages to the LLM.
- **Tool calling:** let a model request a well-defined function, such as looking up order status.
- **Agent:** a system that plans/co-ordinates steps toward a goal, potentially using tools.
- **MCP:** a protocol for exposing tools/context to compatible clients.

This project uses a fixed workflow, not an autonomous agent. MCP is not required for Drools or ML.

## Example walkthrough
Input: “Production is down. 500 users cannot log in.”
1. ML predicts a category and demonstration priority.
2. Drools applies policy, assigns a team, and marks the ticket for approval.
3. Spring AI asks Ollama to draft a reply.
4. The API returns the decision and suggested reply.

## Not production-ready
The dataset is tiny; priority mapping is simplistic; there is no authentication, database, durable approval flow, RAG/tools/MCP, or comprehensive observability/retry setup. Verify dependency compatibility and build in your environment before using.
