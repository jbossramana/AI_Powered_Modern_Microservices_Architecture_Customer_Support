# Spring AI Java Object Demo

A minimal Spring Boot project showing how to:
1. Accept a `Customer` Java record in a REST request.
2. Serialize it to JSON with Jackson.
3. Send the data to an Ollama LLM through Spring AI `ChatClient`.
4. Convert the model response into a `CustomerSummary` Java record using `.entity(CustomerSummary.class)`.

## Requirements
- JDK 21
- Maven 3.9+
- Ollama installed and running
- Maven repository access for the configured Spring Boot and Spring AI versions

## Start Ollama model

```bash
ollama pull qwen2.5
ollama serve
```

If Ollama is already running as a service, you do not need to run `ollama serve` again.

## Run the application

```bash
mvn spring-boot:run
```

## Test from Windows PowerShell

curl.exe -X POST "http://localhost:8080/api/customers/analyze" -H "Content-Type: application/json" -d "{\"name\":\"Ravi\",\"age\":30,\"city\":\"Hyderabad\"}"


Or use Postman with POST `http://localhost:8080/api/customers/analyze`, header `Content-Type: application/json`, and body:

```json
{
  "name": "Ravi",
  "age": 30,
  "city": "Hyderabad"
}
```

Example response (actual wording depends on the model):

```json
{
  "summary": "Ravi is a 30-year-old customer living in Hyderabad.",
  "recommendation": "Consider services that fit the customer's location and needs."
}
```

## Key classes
- `Customer.java`: input Java object.
- `CustomerService.java`: converts the object to JSON, calls `ChatClient`, and maps the response.
- `CustomerSummary.java`: output Java object.
- `CustomerController.java`: REST endpoint.

Note: Model output is generated and may vary. This project has not been compiled or run in this environment.
