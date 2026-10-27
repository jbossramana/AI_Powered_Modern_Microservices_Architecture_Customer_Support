# Customer Support: Query Transformation RAG + MCP

A two-application Spring Boot example demonstrating:

1. **Query Transformation RAG** — rewrite a customer's question before vector similarity search.
2. **MCP tools** — call order/ticket tools exposed by a separate MCP server.
3. **Grounded answers** — use retrieved knowledge-base context and allow the LLM to call MCP tools when relevant.

## Architecture

```text
Customer / curl
      |
      v
RAG + MCP Client (port 8080)
  1. Rewrite query with LLM
  2. Search SimpleVectorStore
  3. Send retrieved context + question to LLM
  4. LLM can invoke MCP tools if relevant
      |
      | Streamable HTTP MCP
      v
MCP Server (port 8081)
  - getOrderStatus(orderId)
  - getTicketDetails(ticketId)
```

## Requirements

- Java 21+
- Maven 3.9+
- Ollama installed and running at `http://localhost:11434`
- Pull these models:

```bat
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

The versions in the POMs are set to Spring Boot `4.1.0` and Spring AI `2.0.1`, matching the target stack for this example. This source bundle has **not been built or integration-tested in your environment**; if your local repositories do not contain these versions, use versions available to your installation and keep Spring AI artifacts aligned through its BOM.

## Run on Windows

Open two terminals in the project root.

### Terminal 1 — start MCP server

```bat
cd mcp-server
mvn spring-boot:run
```

MCP tools are exposed at the server's configured Streamable HTTP endpoint (default `/mcp`) on port `8081`.

### Terminal 2 — start RAG + MCP client

Start this only after the MCP server is running:

```bat
cd rag-mcp-client
mvn spring-boot:run
```

The client connects to `http://localhost:8081` using Streamable HTTP.

## Test the customer support API

```bat
curl -X POST http://localhost:8080/api/support/ask -H "Content-Type: application/json" -d "{\"question\":\"I was charged twice for order ORD-1001. Can you check the order and explain the refund policy?\"}"
```

Another example that should be relevant to the ticket tool:

```bat
curl -X POST http://localhost:8080/api/support/ask -H "Content-Type: application/json" -d "{\"question\":\"Please check ticket TCK-2001 and tell me its status.\"}"
```

The JSON response includes:
- `originalQuery`
- `transformedQuery`
- `retrievedDocumentCount`
- `sources`
- `answer`

MCP tool calls are initiated by the LLM when it decides they are relevant. The model may not invoke a tool for every phrasing, so use explicit questions like “check order ORD-1001” or “look up ticket TCK-2001” when testing.

## MCP tools in this demo

The server returns **sample data only**:
- `getOrderStatus(orderId)` — sample order status and payment information.
- `getTicketDetails(ticketId)` — sample support ticket status and summary.

Replace these methods with real, authenticated service/database calls before using this pattern in an actual support system.

## Important notes

- `SimpleVectorStore` is in-memory and intended for demonstration/testing. The sample knowledge base is loaded at client startup; restarting the client rebuilds it.
- The rewrite prompt asks for only the search query. The client also applies lightweight cleanup, but prompt-based generation is not a formal guarantee of perfectly formatted output.
- MCP HTTP endpoints are not authenticated in this demo. Keep both apps on localhost for learning; add authentication/authorization and network controls before exposing an MCP server outside a trusted environment.
- This sample is educational, not production-ready.
