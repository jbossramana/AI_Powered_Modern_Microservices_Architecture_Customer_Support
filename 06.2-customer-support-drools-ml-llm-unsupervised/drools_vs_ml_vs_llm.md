# Drools vs. Machine Learning (ML) vs. Large Language Models (LLMs)

## 1. Overview

Drools, traditional Machine Learning (ML), and Large Language Models
(LLMs) can all help an enterprise application make decisions, but they
solve different problems.

-   **Drools** applies explicit business rules written by people.
-   **ML** learns patterns from historical examples and predicts a
    category, score, or value.
-   **LLMs** understand and generate natural-language text, summarize
    information, and help users interact conversationally.

They are not necessarily alternatives. In a customer-support
application, they can work together: ML classifies a ticket, Drools
applies business policy, and an LLM drafts a response.

## 2. What is Drools?

**Drools** is a business-rule management system and rule engine.
Developers or business analysts define rules explicitly, and the engine
evaluates facts against those rules.

### How it works

1.  The application gathers facts about a case.
2.  The facts are inserted into a Drools session.
3.  Drools evaluates the rules.
4.  Matching rules execute their consequences, such as assigning a team,
    escalating a case, or requiring approval.
5.  The application uses the updated facts to continue processing.

### Example: customer-support rules

Suppose the application has these facts:

-   Category: `PRODUCTION_INCIDENT`
-   Priority: `CRITICAL`
-   Enterprise customer: `true`
-   Confidence from ML: `0.52`

A rule could say:

``` java
rule "Escalate critical production incidents"
when
    $ticket : TicketFacts(
        category == "PRODUCTION_INCIDENT",
        priority == "CRITICAL"
    )
then
    $ticket.setAssignedTeam("INCIDENT_RESPONSE");
    $ticket.setEscalate(true);
    $ticket.setHumanApprovalRequired(true);
    $ticket.setReason("Critical production incident requires escalation");
end
```

This is an illustrative Drools rule; the class and property names must
match the actual Java model in the project.

### Strengths of Drools

-   Rules are explicit and can be reviewed.
-   Good for policies, thresholds, routing, eligibility, and approval
    requirements.
-   The same inputs and rule version can produce consistent decisions.
-   Business policy can often be changed without retraining an ML model.
-   The reason for a decision can be recorded in the rule consequence.

### Limitations of Drools

-   Someone must define and maintain the rules.
-   It does not learn new rules from examples by itself.
-   Large, overlapping rule sets can become difficult to maintain.
-   It is not designed to interpret arbitrary natural language as
    flexibly as an LLM.

### Use Drools when

A decision must follow a known, explicit policy---for example, "If the
incident is critical, escalate it" or "If approval is required, do not
perform the action until a human approves it."

------------------------------------------------------------------------

## 3. What is Machine Learning (ML)?

**Machine Learning** is a set of methods that learn patterns from data.
A trained model uses those patterns to make predictions on new inputs.

For supervised learning, training data contains examples with known
answers. For example, past support tickets might be labeled `BILLING`,
`ACCOUNT_ACCESS`, or `PRODUCTION_INCIDENT`.

### How it works

1.  Collect representative historical examples.
2.  Label the examples with the desired outcome.
3.  Convert the data into features that the algorithm can use.
4.  Train and evaluate the model.
5.  Deploy the trained model.
6.  Send new tickets to the model to obtain predictions.
7.  Monitor prediction quality and retrain when needed.

### Example: ticket classification

Input ticket:

> "Production is down and users cannot log in."

A trained model might return:

``` json
{
  "category": "PRODUCTION_INCIDENT",
  "priority": "CRITICAL",
  "confidence": 0.82
}
```

This is an illustrative result, not a guaranteed prediction. A real
model's output depends on its training data, features, calibration, and
implementation. A confidence value is not automatically a guarantee that
the prediction is correct.

### Common ML task types

-   **Classification:** predicts a category, such as ticket type or
    priority.
-   **Regression:** predicts a numeric value, such as estimated
    resolution time.
-   **Clustering:** groups similar tickets without requiring a
    predefined label for each group.
-   **Anomaly detection:** identifies unusual patterns, such as a sudden
    spike in failed logins.
-   **Ranking:** orders possible solutions or tickets by relevance or
    urgency.

### Common algorithms

-   Logistic regression
-   Decision trees
-   Random forests
-   Gradient boosting, including XGBoost
-   Support vector machines
-   Neural networks

The algorithm should be selected based on the problem, data, quality
requirements, interpretability, and operating constraints---not simply
because one algorithm is popular.

### Strengths of ML

-   Learns patterns from historical data.
-   Can classify or score large volumes of cases quickly.
-   Can identify patterns that are hard to express as a short list of
    rules.
-   Can be evaluated using measurable metrics such as precision, recall,
    F1 score, and calibration.

### Limitations of ML

-   Requires suitable data and a reliable training/evaluation process.
-   Can make incorrect predictions, including on unfamiliar cases.
-   Can inherit bias or quality problems from its data.
-   May require monitoring and retraining as data and business behavior
    change.
-   Does not automatically explain its reasoning in a human-readable
    way.

### Use ML when

The application needs a prediction based on examples---for example,
classifying a ticket from its text, predicting likely resolution time,
or detecting unusual ticket patterns.

------------------------------------------------------------------------

## 4. What is an LLM?

A **Large Language Model (LLM)** is a model trained on large amounts of
text and other data to process and generate language. Modern LLM
applications often use instructions, conversation context, retrieved
documents, and tool calls to complete tasks.

### How it works in an application

1.  The application constructs a prompt containing instructions and
    relevant context.
2.  It sends the prompt to the LLM.
3.  The LLM generates a response.
4.  The application validates the response and decides what to do with
    it.

An LLM may be hosted locally, for example through Ollama, or accessed
through a remote model provider.

### Example: drafting a support reply

Input:

-   Ticket: "Production is down. 500 users cannot log in."
-   ML category: `PRODUCTION_INCIDENT`
-   Priority: `CRITICAL`
-   Drools decision: assign to `INCIDENT_RESPONSE` and require human
    approval.

The LLM might draft:

> We're sorry for the disruption. We've identified this as a critical
> production incident and are routing it to our incident-response team.
> Our team is investigating the login issue. We'll share an update as
> soon as more information is available.

The application should supply only verified facts. The LLM should not
invent an incident status, resolution estimate, refund, or promise that
has not been authorized.

### Strengths of LLMs

-   Understand and generate natural-language text.
-   Summarize long conversations and documents.
-   Explain technical information in simpler language.
-   Draft personalized responses based on approved context.
-   Extract structured information from text when appropriately
    constrained.
-   Use tools or retrieve knowledge when the application is designed to
    support those capabilities.

### Limitations of LLMs

-   Can produce plausible but incorrect statements, often called
    hallucinations.
-   Do not guarantee consistent outputs for every equivalent request.
-   Can be sensitive to prompt wording and the context supplied.
-   May require safeguards for privacy, security, latency, and cost.
-   Should not be treated as the sole authority for high-impact business
    actions.
-   Need access controls and validation when using retrieved data or
    tools.

### Use an LLM when

The application needs language understanding or generation---for
example, summarizing a customer conversation, drafting a reply, or
explaining a troubleshooting procedure using approved documentation.

------------------------------------------------------------------------

## 5. Side-by-side comparison

  -----------------------------------------------------------------------
  Dimension         Drools            Traditional ML    LLM
  ----------------- ----------------- ----------------- -----------------
  Main purpose      Apply explicit    Predict from      Understand and
                    rules             learned patterns  generate language

  Knowledge source  Rules written by  Training examples Model training
                    people            and learned       plus
                                      parameters        prompt/context
                                                        and optional
                                                        retrieved data

  Typical input     Structured facts  Features, text,   Instructions and
                                      or other model    context, often
                                      inputs            natural language

  Typical output    Rule-driven       Category, score,  Text, summary,
                    actions or        number, ranking,  structured
                    updated facts     or anomaly score  response, or
                                                        tool-call
                                                        proposal

  How it improves   Update and test   Train, evaluate,  Change model,
                    rules             and deploy a      prompt, context,
                                      model             tools, or
                                                        retrieval setup

  Repeatability     Usually highly    Often predictable Output can vary;
                    predictable for   for a fixed model deterministic
                    the same rule     and input,        settings do not
                    version and       depending on      guarantee factual
                    facts, subject to implementation    correctness
                    execution design                    

  Best for          Policy and        Pattern           Conversation,
                    business          recognition and   summarization,
                    constraints       prediction        explanation, and
                                                        drafting

  Main risk         Incorrect,        Wrong             Incorrect or
                    conflicting, or   predictions,      unsupported
                    outdated rules    drift, biased     generated content
                                      data              

  Example in        Critical tickets  Predict ticket    Draft a clear
  support           must be escalated category          customer response
  -----------------------------------------------------------------------

## 6. One customer-support system using all three

A practical architecture can combine ML, Drools, and an LLM.

``` text
Customer submits a ticket
          |
          v
Spring Boot application
          |
          v
ML classifier
Predict category, priority, confidence
          |
          v
Drools rule engine
Apply escalation, routing, and approval policies
          |
          v
Knowledge retrieval (optional RAG)
Find approved troubleshooting or policy documents
          |
          v
LLM
Draft a response using verified ticket facts and context
          |
          v
Application validation and safety checks
          |
          v
Human approval when required
          |
          v
Send response / update ticket / assign team
```

This diagram represents a design pattern. A real application may change
the order, skip retrieval for some requests, or call tools at different
stages.

### Step-by-step example

**Step 1 --- Receive the ticket**

``` json
{
  "ticketId": "TCK-2001",
  "customerId": "C-1001",
  "ticketText": "Production is down. 500 users cannot log in.",
  "enterpriseCustomer": true
}
```

**Step 2 --- ML predicts the ticket type**

The classifier analyzes the ticket and predicts a category and possibly
a priority. The application should retain the model's confidence or
uncertainty information where available.

**Step 3 --- Drools applies business rules**

Drools checks the prediction and customer facts against explicit policy.
For a critical production incident, it can assign the incident-response
team, set escalation flags, and require approval.

**Step 4 --- Retrieve relevant knowledge (optional)**

A retrieval-augmented generation (RAG) component can search approved
incident playbooks or support documentation. The LLM can then draft a
response grounded in that retrieved context.

**Step 5 --- LLM drafts a reply**

The LLM writes a helpful, understandable response. It should not decide
on its own to bypass escalation or human approval.

**Step 6 --- Validate and act**

Spring Boot validates the output, enforces authorization and approval
requirements, and invokes the appropriate business operation. A human
reviews actions that require approval.

------------------------------------------------------------------------

## 7. Who should make which decision?

A useful division of responsibility is:

  -----------------------------------------------------------------------
  Question                Recommended component   Why
  ----------------------- ----------------------- -----------------------
  What kind of ticket is  ML                      Learns patterns from
  this?                                           labeled examples

  What priority or        ML, followed by policy  Produces a prediction
  category is likely?     checks                  that rules or workflow
                                                  can validate

  Must a critical ticket  Drools                  Applies explicit
  be escalated?                                   business policy

  Which team should       Drools                  Implements defined
  receive the ticket                              routing logic
  under known routing                             
  rules?                                          

  Is human approval       Drools or explicit      Enforces a clear
  required for this       application policy      control
  action?                                         

  Can you summarize the   LLM                     Generates
  conversation?                                   natural-language
                                                  summaries

  Can you draft a         LLM                     Generates a
  response?                                       natural-language reply

  Can you answer using    Retrieval plus LLM      Retrieves relevant
  internal documentation?                         knowledge and generates
                                                  a grounded response

  Can the system send a   Application workflow    Keeps permissions and
  reply or make a         with authorization and  side effects outside
  consequential change?   approval controls       unconstrained text
                                                  generation
  -----------------------------------------------------------------------

These are recommendations, not rigid technical boundaries. For example,
an ML model can predict priority, but an explicit policy can override
that prediction when a known safety or business constraint applies.

## 8. How this fits Spring Boot, Spring AI, Drools, and Ollama

In a Java application, the responsibilities may be separated into
services:

-   **Spring Boot:** API endpoints, dependency injection, validation,
    orchestration, and integration.
-   **ML classifier service:** predicts ticket category, priority, or
    confidence. It might be a separate Python service or a model
    embedded in Java.
-   **Drools configuration and rules:** builds the rule engine and
    evaluates `TicketFacts`.
-   **Spring AI:** integrates an LLM into the Java application.
-   **Ollama:** runs supported models locally and exposes an API to
    applications.
-   **Optional RAG:** retrieves approved documents and supplies relevant
    passages to the LLM.
-   **Optional tools or agents:** allow the application to invoke
    defined operations; access and authorization should remain enforced
    by the application.

Spring AI is an integration framework, not itself the LLM. Ollama is the
local model-serving layer, not a substitute for Drools or the ML
classifier.

### Example responsibility flow in Java

``` java
// 1. Predict from the ML service
var prediction = ml.predict(request.ticketText());

// 2. Create facts for business rules
var facts = new TicketFacts(
    prediction.category(),
    prediction.priority(),
    prediction.confidence(),
    request.enterpriseCustomer()
);

// 3. Evaluate Drools rules
KieSession session = kieBase.newKieSession();
try {
    session.insert(facts);
    session.fireAllRules();
} finally {
    session.dispose();
}

// 4. Draft a response using the final, policy-checked facts
String reply = llm.draftReply(request.ticketText(), facts);

// 5. Validate the response and follow approval policy before any action
```

This is illustrative pseudocode based on common APIs; constructor
signatures and configuration must match the actual project. The
application should explicitly enforce approval before performing any
protected side effect.

## 9. Where RAG, tools, agents, and MCP fit

These terms are related to an enterprise AI application but are not
synonyms for ML, Drools, or an LLM.

-   **RAG (Retrieval-Augmented Generation):** retrieves relevant
    documents and supplies them as context to an LLM. It helps ground
    responses in company knowledge, but does not guarantee correctness.
-   **Tools/function calling:** gives an LLM-enabled workflow a defined
    way to request operations, such as looking up a ticket or checking
    an order. The application still needs to validate inputs and
    permissions.
-   **Agent:** a workflow that uses a model to select or sequence steps,
    potentially invoking tools and checking results. An agent is an
    application pattern, not a separate kind of business rule engine.
-   **MCP (Model Context Protocol):** a protocol for connecting AI
    applications to external tools and context providers. It does not
    replace the LLM, ML model, or Drools rule engine.

For example, an LLM may request a tool call to fetch ticket details. The
application fetches only data the caller is authorized to access. Drools
can then apply escalation policy, and ML can predict a category from the
ticket content.

## 10. Common mistakes

1.  **Using an LLM for every decision.** An LLM can draft a response,
    but explicit policies such as approval requirements should be
    enforced by deterministic application logic or rules.
2.  **Treating ML confidence as certainty.** Confidence is a model
    output, not proof that a prediction is correct.
3.  **Expecting Drools to learn from data.** Rules must be authored and
    maintained; learning patterns from examples is an ML task.
4.  **Expecting an LLM to know current private company information
    automatically.** Provide approved context through retrieval or
    authorized tools.
5.  **Letting generated text trigger unrestricted actions.** Validate
    outputs, authenticate the caller, enforce permissions, and require
    human approval where appropriate.
6.  **Mixing policy and prediction without clear precedence.** Define
    what happens when ML is uncertain or conflicts with business rules.
7.  **Ignoring evaluation and monitoring.** Measure classifier quality,
    test rules, evaluate groundedness and usefulness of generated
    responses, and monitor operational failures.

## 11. Testing and measuring quality

Each component needs its own evaluation method.

### ML

Track appropriate metrics such as precision, recall, F1 score, confusion
matrix, and calibration. For critical incidents, pay particular
attention to false negatives: critical tickets incorrectly classified as
non-critical.

### Drools

Create tests for each important rule, boundary condition, and
combination of facts. Confirm that critical tickets escalate, approval
flags are set when expected, and unrelated tickets are not escalated
accidentally.

### LLM

Evaluate factual accuracy, relevance, tone, privacy, and whether the
response is supported by the supplied context. Test adversarial tickets
and instructions that attempt to bypass policy.

### End-to-end workflow

Test the full path from ticket intake to final action. Confirm that
low-confidence predictions are handled safely, rule decisions survive
the LLM step, and approval-required actions cannot occur without
approval.

## 12. Quick decision guide

Choose **Drools** when: - The requirement is an explicit "if this, then
that" policy. - Decisions need to be auditable and consistent. -
Escalation, routing, eligibility, or approval logic must be enforced.

Choose **ML** when: - The system needs to predict a label, score, or
numeric value from examples. - Historical data is available to train and
evaluate a model. - Prediction quality can be measured and monitored.

Choose an **LLM** when: - The system needs to understand or generate
language. - It needs to summarize conversations, explain procedures, or
draft responses. - It benefits from natural-language interaction and
contextual reasoning, with appropriate validation.

Choose a **combination** when the application needs all three:
prediction, enforceable business policy, and natural-language
interaction.

## 13. Final takeaway

Think of the three roles this way:

-   **ML predicts:** "This ticket is probably a critical production
    incident."
-   **Drools enforces policy:** "Critical production incidents must be
    escalated and require the specified approval."
-   **The LLM communicates:** "Here is a clear, empathetic response
    based on the verified incident details."

The safest enterprise design makes each component responsible for the
job it handles best, then coordinates them through an application
workflow with validation, logging, permissions, and human approval where
needed.


Which one should you use?

Requirement																	Best choice
Simple, fixed condition														Java if-else
Many business rules with priorities and exceptions				Drools
Learn patterns from historical data									ML
Understand customer messages and generate responses		LLM


Example:

Customer-support scenario														Best approach						Why?
If priority is CRITICAL, escalate														Drools					The rule is known
Predict priority from thousands of past tickets									ML							Learns patterns from examples
Understand “I can't get into my account”										ML or LLM				Interprets unstructured text
Draft a helpful customer reply														LLM						Generates natural language


