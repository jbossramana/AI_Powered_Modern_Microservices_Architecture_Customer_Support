WINDOWS CMD CURL COMMANDS
Spring AI + Ollama — Customer Support Capstone

IMPORTANT
- Run these commands in Windows Command Prompt (CMD).
- Start the Spring Boot app on port 8080 first.
- Ensure Ollama is running at http://localhost:11434.
- The original Document Summarizer and the Customer Support Capstone use different request formats for /api/documents/summarize.

============================================================
1. ORIGINAL DOCUMENT SUMMARIZER — UPLOAD A TEXT FILE
============================================================

Your sample file:
D:\micro\usecase_task\13-spring-ai-llm-document-summarizer\sample-documents\customer-complaint.txt

Command:
curl -X POST "http://localhost:8080/api/documents/summarize" -H "Content-Type: application/json" -d "{\"text\":\"Customer ordered a laptop. Payment succeeded. Delivery is delayed. Customer contacted support twice and may request a refund.\"}"

Do NOT add Content-Type: application/json to this multipart upload command.

============================================================
2. CAPSTONE API COMMANDS
============================================================

A. Health check
curl http://localhost:8080/actuator/health

B. Classify a support ticket
curl -X POST "http://localhost:8080/api/tickets/classify" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-10025\",\"customerMessage\":\"My laptop delivery is late and I am very frustrated.\"}"

C. Summarize text (capstone JSON endpoint)
curl -X POST "http://localhost:8080/api/documents/summarize" -H "Content-Type: application/json" -d "{\"text\":\"Customer ordered a laptop. Payment succeeded. Delivery is late. Customer contacted support twice and may request a refund.\"}"

D. Ask the knowledge base (RAG)
curl -X POST "http://localhost:8080/api/knowledge/ask" -H "Content-Type: application/json" -d "{\"question\":\"What should support do when a shipment is delayed?\"}"

E. Let the LLM select a read-only tool
curl -X POST "http://localhost:8080/api/tool-calling" -H "Content-Type: application/json" -d "{\"question\":\"Check payment and order status for ORD-10025, then explain the situation.\"}"

F. Investigate an order
curl -X POST "http://localhost:8080/api/investigations" -H "Content-Type: application/json" -d "{\"orderId\":\"ORD-10025\",\"productName\":\"laptop\",\"customerMessage\":\"My laptop is late. Payment was successful. What should happen next?\"}"

G. Generate a response draft
curl -X POST "http://localhost:8080/api/responses/draft" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-10025\",\"customerMessage\":\"My laptop is late. Please confirm delivery.\"}"

H. Approve a response draft
Run after generating a draft for TCK-10025:
curl -X POST "http://localhost:8080/api/responses/approval" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-10025\",\"approvedBy\":\"team-lead\",\"responseText\":\"We apologize for the delay. Our team is checking the shipment status and will provide a confirmed update.\",\"approved\":true}"

I. Reject a response draft
Run after generating a draft for TCK-10025:
curl -X POST "http://localhost:8080/api/responses/approval" -H "Content-Type: application/json" -d "{\"ticketId\":\"TCK-10025\",\"approvedBy\":\"team-lead\",\"responseText\":\"Draft rejected.\",\"approved\":false}"

J. Start a conversation
curl -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"conversationId\":\"demo-1\",\"message\":\"My order ORD-10025 is late.\"}"

K. Continue the same conversation
curl -X POST "http://localhost:8080/api/chat" -H "Content-Type: application/json" -d "{\"conversationId\":\"demo-1\",\"message\":\"What did I tell you about my order?\"}"

L. View demo data
curl http://localhost:8080/api/demo-data

============================================================
3. VERIFY OLLAMA
============================================================

List installed models:
ollama list

Check Ollama API:
curl http://localhost:11434/api/tags

Download chat model:
ollama pull qwen2.5:7b

Download embedding model (used by RAG):
ollama pull nomic-embed-text

============================================================
4. IMPORTANT DIFFERENCE BETWEEN PROJECTS
============================================================

Original Document Summarizer:
- Endpoint: POST /api/documents/summarize
- Request format: multipart/form-data
- Form field: file
- Example uses -F "file=@path-to-file.txt"

Customer Support AI Capstone:
- Endpoint: POST /api/documents/summarize
- Request format: application/json
- JSON field: text
- Example uses -H "Content-Type: application/json" -d "{\"text\":\"...\"}"

These are different implementations. Run the project whose command you are using.
The same route cannot have both request formats in the same controller mapping unless
you explicitly configure separate mappings.

