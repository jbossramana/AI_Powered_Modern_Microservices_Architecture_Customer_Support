# MCP Server — Enterprise Systems

Exposes three tools (`getCustomer`, `getTicketHistory`, `getPlanSla`) over the Model
Context Protocol via SSE on port **8081**. See the parent `04-mcp-spring-ai/README.md`
for how this pairs with `mcp-client`.

## Run it standalone

```bash
cd mcp-server
mvn spring-boot:run
```

No Ollama needed for this half — it doesn't call a model itself, it only serves tools.

## Verify it's up

```bash
curl -s http://localhost:8081/sse
```

You should see an SSE stream open (it will hang — that's correct, `Ctrl+C` to stop). A
real check is easiest from the client module or an MCP inspector tool; this module is
meant to be driven by `mcp-client`, not called directly with `curl`.
