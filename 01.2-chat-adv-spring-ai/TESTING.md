# Testing checklist (Windows CMD)

From the folder containing `pom.xml`:

```cmd
mvn clean test
mvn spring-boot:run
```

With Ollama running and `llama3.2` downloaded:

```cmd
curl.exe -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"message\":\"Explain MFA in one sentence.\"}"
curl.exe -X POST "http://localhost:8080/api/chat/demo" -H "Content-Type: application/json" -d "{\"message\":\"My name is Ravi. Remember it.\"}"
curl.exe -X POST "http://localhost:8080/api/chat/demo" -H "Content-Type: application/json" -d "{\"message\":\"What is my name?\"}"
curl.exe -X POST "http://localhost:8080/api/chat/tools" -H "Content-Type: application/json" -d "{\"message\":\"What are your support hours?\"}"
curl.exe -X POST "http://localhost:8080/api/support/ask" -H "Content-Type: application/json" -d "{\"question\":\"How do I reset my password?\"}"
curl.exe -X POST "http://localhost:8080/api/tickets/triage" -H "Content-Type: application/json" -d "{\"message\":\"Our team cannot log in after an outage.\"}"
curl.exe "http://localhost:8080/actuator/health"
```

LLM wording and classifications vary by model.
