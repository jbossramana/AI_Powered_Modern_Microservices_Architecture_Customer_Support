# Test Checklist

Run `mvn clean test` first. Start Ollama and pull `llama3.2:latest` before exercising model-backed endpoints.

## Manual test cases

1. **Basic chat:** `POST /api/chat` with `{"message":"How do I reset my password?"}`. Expect JSON with a non-empty `reply`.
2. **Memory:** send two requests to `/api/chat/lab-session`, first stating a harmless fact and then asking about it. The second may recall it. This is model-dependent; use the same session ID.
3. **Session isolation:** repeat the memory question using `/api/chat/another-session`. It should not intentionally share the first session's memory.
4. **Tool:** ask `/api/chat` for support hours or status of `TCK-1002`. Inspect application logs and answer for a tool-informed response. Tool selection is model-dependent.
5. **RAG known topic:** ask `/api/support/ask` about password reset. Expect a `sources` array and source IDs in the answer where the model follows instructions.
6. **RAG unknown topic:** ask about an unrelated topic with no keyword overlap. Expect the safe fallback and an empty source array.
7. **Ingest:** add a new item with `POST /api/knowledge`, then ask about a unique phrase in it through `/api/support/ask`.
8. **Triage:** post a production outage description to `/api/tickets/triage`. Expect fields `category`, `priority`, `sentiment`, `summary`, `recommendedActions`; values depend on the model.
9. **Validation:** send `{"message":""}` to `/api/chat`. Expect HTTP 400 due to validation.
10. **Health:** `GET /actuator/health`. Expect `{"status":"UP"}` when the application is running.

## PowerShell example

```powershell
$body = @{ message = "Three production regions are down and customers cannot log in." } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/tickets/triage -ContentType 'application/json' -Body $body
```

## Important

Tests involving LLM output are probabilistic and may vary. Do not assert exact wording. Unit tests should focus on deterministic code and integration tests should check schema/behavior ranges rather than one exact response.
