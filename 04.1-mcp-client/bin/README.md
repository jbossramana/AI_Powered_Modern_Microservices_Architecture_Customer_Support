# MCP Client — Support Chat App

The same style of chat app as Module 1, except its tools come from the `mcp-server`
module over the network via MCP/SSE instead of a local `@Component`. Runs on port
**8080**. See the parent `04-mcp-spring-ai/README.md` to run both together.

## Run it (after mcp-server is already running)

```bash
cd mcp-client
mvn spring-boot:run
```

## Try it

curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" -d "{\"message\":\"What is the SLA for customer C-1002, and do they have any tickets?\"}" 
