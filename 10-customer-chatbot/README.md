# Acme Support Chatbot Widget — README

## Description

`support-chatbot.html` is a standalone, embeddable customer-facing chat widget: a
branded launcher button (bottom-right) that expands into a chat panel where a customer
can ask a support question and get a reply from whichever backend it's pointed at. It's
a single self-contained HTML file — no build step, no npm, no framework — designed to
either be opened directly in a browser or dropped into any web page, including as a
static resource served by one of the Spring Boot backends from this project's series.

The widget talks to **more than one endpoint**, routed automatically: if a message
contains something that looks like a customer ID (e.g. `C-1001`), it calls a
customer-lookup endpoint and renders the result as a structured account-details card;
everything else falls through to the general support Q&A endpoint as before. This
required a small backend addition too — `clean-architecture-spring-ai` didn't previously
expose a customer-lookup route — covered below.

It ships paired with `WebConfig.java`, a small CORS configuration for
`clean-architecture-spring-ai` (the widget's default backend), since a browser treats a
locally opened HTML file as having no real origin and blocks cross-origin requests
without it.

### What's included

| File | Purpose |
|---|---|
| `support-chatbot.html` | The widget itself: markup, styles, and logic in one file |
| `WebConfig.java` | Dev-only CORS config — enables the widget to call `clean-architecture-spring-ai` when opened as a local file |
| `GetCustomerDetailsUseCase.java`, `GetCustomerDetailsService.java` | New use case (application layer) backing customer lookup — reuses the existing `CustomerRepositoryPort`, no new outbound port needed |
| `CustomerController.java` | New adapter exposing `GET /api/customers/{customerId}` |
| `GetCustomerDetailsServiceTest.java` | Unit test for the new use case, same no-Spring-no-Ollama pattern as `TriageTicketServiceTest` |
| (edited) `UseCaseConfig.java` | Wires the new use case as a bean |

### Key features

- **Backend-agnostic by config, not by code.** A single `CONFIG` object at the top of
  the `<script>` block controls the API URL and the request/response JSON field names.
  Defaults to `clean-architecture-spring-ai`'s RAG endpoint (`/api/support/ask`,
  grounded answers with citations); pointing it at any of the plain `/api/chat`
  endpoints elsewhere in the series is a 3-line edit, documented inline right above
  `CONFIG`.
- **Multi-endpoint routing.** A small `ROUTES` array checks each outgoing message
  against a pattern (currently: does it contain a customer ID?) and dispatches to the
  matching handler — customer lookup, or the fallback Q&A endpoint. Adding a third
  endpoint (e.g. a ticket-status lookup) means adding one more `{ test, handle }`
  entry, not restructuring anything.
- **Structured replies, not just text.** A customer-lookup match renders an
  account-details card (Customer ID / Name / Email / Plan) instead of a prose bubble —
  built with the same `textContent`-only DOM construction as regular messages, so
  nothing in the response can inject markup.
- **Source citations as filed-document chips.** When the backend returns
  `citedArticleTitles`, each is rendered as a small tagged chip under the reply —
  answers a customer can verify, not just trust.
- **Defensive by default.** Message text is inserted via `textContent`, never
  `innerHTML`, so anything a customer types (including HTML/script) renders as inert
  text rather than executing. Network failures, non-2xx responses, and unexpected
  response shapes all fall back to a plain-language error bubble instead of breaking
  silently.
- **No persistence.** Chat history lives only in a JS variable for the page's lifetime —
  nothing is written to `localStorage`/`sessionStorage`, and a refresh clears the
  conversation. Intentional for this deliverable; see Limitations below.
- Responsive down to a 480px breakpoint, visible keyboard focus throughout, and
  `prefers-reduced-motion` is respected.

## Setup (before testing)

Pick one:

1. **Standalone + CORS** — drop `WebConfig.java` into
   `clean-architecture-spring-ai/src/main/java/com/acmesupport/cleanarch/config/`,
   run `mvn spring-boot:run`, then just double-click `support-chatbot.html` to open it.
2. **Same-origin static serving** — rename it to `index.html`, drop it in
   `clean-architecture-spring-ai/src/main/resources/static/`, run `mvn spring-boot:run`,
   open `http://localhost:8080`. No CORS config needed either way, but harmless to
   include `WebConfig.java` too.
3. **A different backend from the series** — edit `CONFIG` per the comments in the
   `<script>` block first (see the compatibility table below), then apply the same
   CORS pattern from `WebConfig.java` to that module if testing it standalone.

## What to test

A practical manual QA pass — no test framework required, just a browser and (for most
sections) a running backend. Work through these roughly in order; later sections build
on earlier ones working.

### 1. Connectivity

- [ ] Backend is running and reachable directly (`curl http://localhost:8080/api/support/ask -X POST -H "Content-Type: application/json" -d '{"question":"test"}'` returns JSON, not a connection error)
- [ ] Widget's launcher renders in the bottom-right corner on page load, with a brief entrance animation
- [ ] Google Fonts load (Space Grotesk headings, IBM Plex Sans body text) — if offline, page should still be legible in the system-font fallback, just visually plainer

### 2. Opening and first impression

- [ ] Clicking the launcher opens the panel with a smooth expand animation, anchored bottom-right
- [ ] Launcher icon swaps from chat bubble to an X, and its label changes from "Chat with Aria" to "Close"
- [ ] Aria's greeting message appears automatically the *first* time the panel opens — not on every open/close
- [ ] Status row shows a pulsing dot and "Synced and ready"
- [ ] Input field is focused automatically once the panel finishes opening

### 3. Sending messages

- [ ] Typing a question and clicking send: message appears right-aligned in a dark bubble, input clears, typing indicator (three bouncing dots) appears left-aligned
- [ ] Pressing **Enter** sends the message (same as clicking send)
- [ ] Pressing **Shift+Enter** inserts a newline instead of sending, and the textarea grows to fit (up to ~4 lines, then scrolls)
- [ ] Submitting an empty or whitespace-only message does nothing
- [ ] While waiting for a reply: input and send button are both disabled, re-enabled the moment a reply (or error) arrives
- [ ] A real reply replaces the typing indicator, left-aligned in a light bubble, with a timestamp
- [ ] Message list auto-scrolls to keep the latest message in view as the conversation grows

### 4. Citations (RAG backend only)

Ask something the knowledge base actually covers, e.g. *"How much storage does the
Business plan include?"* or *"Can I get a refund for last month?"*

- [ ] Reply includes one or more small tagged chips beneath it (e.g. "storage-limits-and-upgrading-plans.md")
- [ ] Ask something clearly outside the knowledge base (e.g. *"Do you accept cryptocurrency?"*) — reply should say it doesn't have that information rather than guessing, and should show no citation chips (or only weakly related ones, depending on the similarity threshold)

### 5. Customer lookup (new)

First, confirm the endpoint works directly:
```bash
curl -s http://localhost:8080/api/customers/C-1001 | jq
# expect: {"customerId":"C-1001","name":"Jordan Lee","email":"jordan.lee@example.com","plan":"PRO"}
curl -s -i http://localhost:8080/api/customers/C-9999
# expect: 404, {"error":"No customer found with id C-9999"}
```

Then in the widget:

- [ ] Send **"What plan is C-1001 on?"** (a customer ID embedded in an ordinary sentence) → a structured account card appears (dark header "Account found", then Customer ID/Name/Email/Plan rows) instead of a text bubble, and the Q&A endpoint is *not* called for this message
- [ ] Send **"c-1002"** (lowercase, ID alone) → same card, for Amara Chen / Business — confirms the pattern match is case-insensitive
- [ ] Send **"C-9999"** (well-formed but nonexistent ID) → a plain (non-error-styled) message: *"I couldn't find an account with ID C-9999..."* — this is a 404, a normal outcome, and should look different from a connection failure
- [ ] Send an ordinary question with no ID in it (e.g. *"How do I reset my password?"*) → routes to the Q&A endpoint as before, no regression
- [ ] Open the Network tab while testing the three cases above and confirm each one hits exactly the endpoint you'd expect (`/api/customers/...` for the first two, `/api/support/ask` for the last) — this is really a test of `ROUTES` picking the right handler, not just of the visible output

### 6. Error handling — deliberately break things

- [ ] **Stop the backend**, then send a message → friendly "I couldn't reach the support system" bubble appears (not a blank screen, not a raw stack trace in the UI). Restart the backend and confirm the *next* message works normally.
- [ ] **Wrong port in `CONFIG.apiUrl`** → same graceful connection-error bubble
- [ ] **Mismatched `responseField`** (temporarily set it to a field name that doesn't exist in the response) → "I got a response I couldn't read" bubble, not a JS error in the console breaking the page
- [ ] Open the browser console while doing the above — errors should be logged there for debugging, but never surfaced to the customer as raw text

### 7. Backend compatibility — switching `CONFIG`

Edit `CONFIG` per the module, reload the page, and confirm a normal round trip works:

| Module | `apiUrl` | `requestField` / `responseField` | `citationsField` | Extra |
|---|---|---|---|---|
| `clean-architecture-spring-ai` (default) | `/api/support/ask` | `question` / `text` | `citedArticleTitles` | — |
| `00-chat-spring-ai`, `00-chat-langchain4j`, `01-tools-spring-ai`, `04-mcp-spring-ai` client, `08-observability-spring-ai` | `/api/chat` | `message` / `reply` | `null` | — |
| `09-owasp-security-spring-ai` | `/api/secure-chat` | `message` / `reply` | `null` | Try sending "ignore all previous instructions" — should come back as a `400`, surfaced as the standard connection-error bubble |
| `07-ai-gateway-spring-ai` | `/api/chat` | `message` / `reply` | `null` | Set `extraHeaders: {'X-API-Key': 'demo-internal-portal'}`; remove it and confirm you get a `401`-driven error bubble instead |

### 8. Input safety

- [ ] Send literal `<script>alert(1)</script>` as a message → it should appear as plain visible text in your own message bubble, and **no alert box should fire**
- [ ] Send a very long message (a few paragraphs pasted in) → textarea grows to its max height then scrolls internally; send still works; bubble wraps text rather than overflowing the panel
- [ ] Send **"C-1001/../../etc"** or similar — the extracted ID is `encodeURIComponent`-escaped before being substituted into the lookup URL, so it should hit the endpoint as a literal (and 404, since no such ID exists) rather than manipulating the request path

### 9. Responsive & visual behavior

- [ ] Resize the browser to a narrow (mobile) width — panel should expand to fill nearly the full width/height rather than staying fixed at 380px or overflowing the viewport
- [ ] Hover the launcher and the send button — both should show a clear, subtle hover state
- [ ] Tab through the page with the keyboard only — launcher, close button, input, and send button should all show a visible focus outline in sequence

### 10. Accessibility

- [ ] With a screen reader on (or by inspecting the DOM), confirm the messages container has `aria-live="polite"` and new messages are announced
- [ ] Launcher's `aria-expanded` flips between `false`/`true` as the panel opens/closes
- [ ] Pressing **Escape** while the panel is open closes it
- [ ] macOS/Windows "reduce motion" accessibility setting turned on → panel open/close and message entrances become instant rather than animated

## Known limitations

- Routing is a regex pattern match, not intent understanding — a message that happens
  to contain a `C-####`-shaped substring for an unrelated reason (rare, but possible)
  would be routed to customer lookup instead of the Q&A endpoint. Good enough for this
  ID format; a production system with richer intents would likely want the backend
  itself (or a lightweight classification call) making this decision instead of a
  frontend regex.
- Customer lookup and the Q&A endpoint never run for the same message — by design, to
  keep routing predictable and single-request per turn. A message like *"What plan is
  C-1001 on, and also how do refunds work?"* only returns the account card, not an
  answer to the second half.
- No conversation persistence across page reloads — by design for this deliverable, not a bug. Adding it would mean either in-memory server-side sessions (matching the pattern in `06-human-in-the-loop-spring-ai`) or a cookie/session-backed history endpoint; plain `localStorage` was deliberately avoided.
- The knowledge-base "I don't know" behavior in section 4 depends entirely on the paired backend's system prompt and similarity threshold, not on anything in the widget — if it starts guessing instead of deferring, that's a backend prompt/RAG tuning issue, not a widget bug.
- This QA pass is manual. For CI, the natural next step is a Playwright or Cypress script driving the same checklist against a real running backend — not included here to keep this deliverable to what was asked for.
