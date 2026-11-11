Customer Support Chat (Spring AI + Groq)

A simple Spring Boot REST API that answers **general customer-support questions** using an LLM through Groq's OpenAI-compatible API.

This is the customer-support version of the original savings-account chat example.

## What it does

- Accepts a customer question at `GET /support/ask`
- Sends the question to the LLM using Spring AI `ChatClient`
- Returns JSON containing the question, answer, use case, and framework

Example questions:
- “I was charged twice for my order. What should I do?”
- “I cannot log in to my account. What can I try?”
- “How can I ask for a refund?”

**Scope and safety:** this is only a basic chat example. It does not read customer/account records, verify identity, open tickets, issue refunds, or change orders. The model must not be treated as a source of company-specific policy unless that policy is supplied to it.

## Technology

- Java 21+
- Spring Boot 4.0.1
- Spring AI 2.0.0
- Groq API (OpenAI-compatible endpoint)
- Maven

## 1. Configure Groq API key

Create an API key in your Groq account, then set it as an environment variable. Do not hard-code the key in `application.yml` or commit it to source control.

**Windows Command Prompt (CMD):**
```bat
set GROQ_API_KEY=your_groq_api_key_here
```

Keep that same CMD window open for the following commands.

**PowerShell:**
```powershell
$env:GROQ_API_KEY = "your_groq_api_key_here"
```

The model configured in `application.yml` is `llama-3.3-70b-versatile`. Check your Groq console if that model identifier is no longer available to your account.

## 2. Run the application

From the directory containing `pom.xml`:

```bat
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

## 3. Test from Windows CMD

Use `curl.exe` and `--data-urlencode` so spaces and punctuation in the question are encoded correctly:

```bat
curl.exe --get "http://localhost:8080/support/ask" --data-urlencode "question=I was charged twice for my order. What should I do?"
```

Another example:

```bat
curl.exe --get "http://localhost:8080/support/ask" --data-urlencode "question=I cannot log in to my account. What should I try?"
```

Expected response shape (the exact wording of the answer varies):
```json
{
  "question": "I was charged twice for my order. What should I do?",
  "answer": "I'm sorry that happened. Check your order and payment history ...",
  "useCase": "Customer Support",
  "framework": "Spring AI"
}
```

## 4. Test from PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/support/ask?question=I%20cannot%20log%20in%20to%20my%20account"
```

## Architecture

```text
Customer
   |
   | GET /support/ask?question=...
   v
Spring Boot REST Controller
   |
   v
Spring AI ChatClient
   |
   v
Groq-hosted LLM
   |
   v
JSON response to customer
```

## Project layout

```text
step1-customer-support-groq/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/example/support/
    │   ├── Step1Application.java
    │   └── springai/CustomerSupportChatController.java
    └── resources/application.yml
```

## What comes next

1. **Tool calling:** safely look up order status or create a support ticket through explicit application tools.
2. **RAG:** answer using approved company FAQs, refund policy, and troubleshooting documents.
3. **Agent workflow:** coordinate multi-step support tasks with clear limits and approval gates.
4. **MCP:** connect to external support tools through an MCP server.

For real customer data or actions, add authentication, authorization, input validation, audit logging, rate limiting, and server-side checks. Never let the LLM alone authorize refunds or account changes.
