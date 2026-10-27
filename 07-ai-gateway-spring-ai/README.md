# Step 7 — AI Gateway (Spring AI)

Every earlier module called Ollama directly from inside the app. This module puts a
gateway in front of it: callers authenticate with an API key, are subject to a per-key
rate limit, get served from a cache when possible, and every call is logged centrally —
the cross-cutting concerns you'd want regardless of *which* AI feature is calling the
model underneath.

## What this teaches

- API-key authentication via a request header, checked against config
- A hand-rolled per-key **token bucket** rate limiter (and why it's keyed per-caller,
  unlike caching)
- Response **caching** keyed on model+prompt, independent of who's asking
- Centralized usage logging as the natural seam for metering, billing, or the
  observability work in Module 8
- Binding a `Map` of nested config to a typed, immutable `@ConfigurationProperties` record

## Prerequisites

```bash
ollama pull qwen3:8b
```

## Run it

```bash
cd 07-ai-gateway-spring-ai
mvn spring-boot:run
```

Two demo API keys are preconfigured in `application.yml`:

| Key | Client | Limit |
|---|---|---|
| `demo-internal-portal` | Internal Support Portal | 20 req/min |
| `demo-partner-integration` | Partner Integration | 5 req/min |

## Try it

**A normal request:**

curl -s http://localhost:8080/gateway/v1/chat -H "X-API-Key: demo-internal-portal" -H "Content-Type: application/json" -d "{\"message\":\"What plans does Acme Cloud Storage offer?\"}" 

**Missing/wrong key** → `401`:

curl -s -i http://localhost:8080/gateway/v1/chat -H "Content-Type: application/json" -d "{\"message\":\"hello\"}"

**Same question again** → notice `"cached": true` and a near-instant response:

curl -s http://localhost:8080/gateway/v1/chat -H "X-API-Key: demo-internal-portal" -H "Content-Type: application/json" -d "{\"message\":\"What plans does Acme Cloud Storage offer?\"}" 


**Blow through the tight-limit key** → `429` after 5 requests within a minute:
```bash
for i in $(seq 1 7); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/gateway/v1/chat \
    -H "X-API-Key: demo-partner-integration" \
    -H "Content-Type: application/json" \
    -d "{\"message\": \"unique question number $i\"}"
done
```

## How it works

Request flow in `GatewayController.chat`: API key lookup → `ApiKeyRateLimiter.tryConsume`
→ `PromptCacheService.get` → (cache miss only) call the model → `PromptCacheService.put`
→ log the call. Any of the first three steps can return early without ever reaching
Ollama.

## What this doesn't cover (and where you'd add it)

- **Multi-instance rate limiting**: `ApiKeyRateLimiter` is in-memory and per-instance.
  Behind a load balancer with N instances, move the same token-bucket logic to Redis so
  the limit is shared.
- **Model routing**: this gateway forwards to one configured Ollama model. A production
  gateway often routes by request "profile" (fast/cheap vs. high-quality) to different
  models or providers — that's a `Map<String, ChatModel>` and a lookup key away from
  what's here.
- **Prompt validation / injection defense**: see `09-owasp-security-spring-ai` — a real
  gateway is often exactly where you'd place that check, before any request reaches a
  model at all.

## What's next

`08-observability-spring-ai` looks at what to do with everything this gateway is
already logging — Micrometer metrics, distributed tracing, and Spring AI's built-in
observation instrumentation.
