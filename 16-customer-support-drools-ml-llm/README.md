# Customer Support AI: Spring Boot + Drools + ML + Spring AI + Ollama

A learning project combining traditional ML for ticket classification, Drools for explicit business rules, and Spring AI + Ollama for drafting customer replies.

## Version note
The POM targets the requested Spring Boot 4.2 / Spring AI 2.0 generation. Verify that these exact versions are published in your Maven repositories before building; adjust the centralized version properties if needed. This ZIP has not been compiled in your local environment.

## Architecture

```text
Windows curl -> Spring Boot REST :8080
                  |--> Python/scikit-learn ML service :8001
                  |--> Drools rules (.drl)
                  |--> Spring AI ChatClient -> Ollama :11434
                  |--> Combined decision + suggested reply
```

## Prerequisites (Windows)
JDK 21+, Maven 3.9+, Python 3.10+, and Ollama.

```bat
java -version
mvn -version
python --version
ollama --version
```

## 1. Start Ollama

```bat
ollama pull qwen3:8b
ollama serve
```

If Ollama is already running, just pull the model. Check:

```bat
curl.exe http://localhost:11434/api/tags
```

## 2. Start the ML service

Open another terminal:

```bat
cd customer-support-ai-spring\ml-service
python -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8001
```

Health check:

```bat
curl.exe http://localhost:8001/health
```

Test prediction:

```bat
curl.exe -X POST "http://localhost:8001/predict" -H "Content-Type: application/json" -d "{\"text\":\"Production is down and users cannot log in\"}"
```

## 3. Build and start Spring Boot

From the root directory containing `pom.xml`, use a third terminal:

```bat
mvn -U clean package
mvn spring-boot:run
```

If Maven cannot resolve Boot/Spring AI versions, select mutually compatible published versions and configure the Spring milestone repository if using milestone/RC artifacts.

## 4. Windows curl examples

Critical production incident:

```bat
curl.exe -X POST "http://localhost:8080/api/tickets/process" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2001\",\"customerId\":\"C-1001\",\"ticketText\":\"Production is down. 500 users cannot log in.\",\"enterpriseCustomer\":true}"
```

Billing:

```bat
curl.exe -X POST "http://localhost:8080/api/tickets/process" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2002\",\"customerId\":\"C-1002\",\"ticketText\":\"I was charged twice for the same invoice.\",\"enterpriseCustomer\":false}"
```

Account access:

```bat
curl.exe -X POST "http://localhost:8080/api/tickets/process" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-2003\",\"customerId\":\"C-1003\",\"ticketText\":\"I forgot my password and cannot log in.\",\"enterpriseCustomer\":false}"
```

The response combines the ML prediction, Drools decisions, and an LLM-generated suggested reply. Exact predictions and wording can vary.

## Troubleshooting
- Port 8001 refused: start the Python service.
- Port 11434 refused: start Ollama and check `/api/tags`.
- Model missing: run `ollama pull qwen3:8b`, or change `application.yml`.
- Maven cannot resolve versions: verify artifact versions and repository configuration.
- ML output is unreliable: expected with the tiny sample dataset; train and evaluate on representative labeled tickets.

## Production cautions
This is a teaching sample. It lacks authentication, persistence, durable approval workflow, RAG, tool calling, and MCP. The approval flag is not a complete approval system. Never let an LLM authorize or directly execute production actions. Add security, validation, tests, timeouts, audit logging, monitoring, and representative ML training data before production use.
