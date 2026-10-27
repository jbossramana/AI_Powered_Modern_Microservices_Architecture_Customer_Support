# Step 8 — Observability (Spring AI)

Adds `spring-boot-starter-actuator` and a Micrometer tracing bridge, which instrument
every Spring AI model call automatically — no code changes needed for that baseline —
plus one custom `SupportMetrics` class showing the layer you *do* need to write:
business-specific metrics the framework has no way to know about on its own.

## What this teaches

- What you get for free: Spring AI's automatic Micrometer Observation instrumentation
  for `ChatModel`/`EmbeddingModel` calls, once actuator + tracing are on the classpath
- What you still write yourself: custom `Counter`/`Timer` metrics tied to business
  meaning (`support.tickets.classified` by category, `support.chat.duration` by outcome)
- Exposing metrics for Prometheus scraping via `/actuator/prometheus`
- Wiring distributed tracing to Zipkin via `management.zipkin.tracing.endpoint`
- An optional `docker-compose.yml` (Prometheus + Grafana + Zipkin) to actually look at
  what's being recorded, instead of only reading raw JSON

## Prerequisites

```bash
ollama pull qwen3:8b
```

## Run it (app only — metrics work without Docker)

```bash
cd 08-observability-spring-ai
mvn spring-boot:run
```

curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" -d "{\"message\":\"How do I reset my password?\"}" 


curl -s http://localhost:8080/actuator/metrics/support.tickets.classified 
curl -s http://localhost:8080/actuator/metrics/support.chat.duration 
curl -s http://localhost:8080/actuator/prometheus | grep support_
curl -s http://localhost:8080/actuator/prometheus | findstr "support_"


Make a few requests with different kinds of questions (billing, technical, feature
request) and re-check `support.tickets.classified` — the `category` tag distribution
shifts accordingly.

## Run it with the full stack (optional)

```bash
docker compose up -d
```

Then:
- **Prometheus** at http://localhost:9090 — try the query `support_tickets_classified_total`
- **Grafana** at http://localhost:3000 (anonymous admin access is enabled for this demo
  — add Prometheus at `http://prometheus:9090` as a data source, don't do this in
  production) to build a dashboard on top of it
- **Zipkin** at http://localhost:9411 — after a few chat requests, search for traces
  from the `08-observability-spring-ai` service to see the model call's span, with its
  duration and any tags Spring AI attached

If `host.docker.internal` doesn't resolve from inside the Prometheus container on your
platform, see the comment in `docker-compose.yml`.

## How it works

- `pom.xml` adds `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
  (metrics export format), `micrometer-tracing-bridge-otel` (bridges Micrometer's
  tracing API to OpenTelemetry), and `opentelemetry-exporter-zipkin` (ships spans to
  Zipkin). This combination is what makes Spring AI's calls show up as traces and
  metrics with no code changes in the AI-calling code itself.
- `SupportMetrics` wraps a `MeterRegistry` with two purpose-built instruments: a
  `Counter` tagged by ticket category, and a `Timer` tagged by outcome (success/error).
- `ChatController` starts the timer before calling the model, records the category once
  it has a classified reply, and stops the timer in a `finally` block so failed requests
  are measured too — an error being silently unmeasured is its own kind of blind spot.

## What's next

`09-owasp-security-spring-ai` closes out the series: prompt-injection defense, PII
redaction, and guarding against system-prompt leakage — the same kind of blocked or
suspicious request this module's metrics would help you notice happening in the first
place.
