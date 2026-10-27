package com.example.support.service;

import com.example.support.model.SupportTicket;
import com.example.support.tools.SupportTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TicketAiService {
    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBase;
    private final AuditService audit;
    private final SupportTools supportTools;

    public TicketAiService(ChatClient.Builder builder,
                           KnowledgeBaseService knowledgeBase,
                           AuditService audit,
                           SupportTools supportTools) {
        this.chatClient = builder.build();
        this.knowledgeBase = knowledgeBase;
        this.audit = audit;
        this.supportTools = supportTools;
    }

    public String summarize(String text) {
        ensureText(text);
        String result = chatClient.prompt()
                .system("Summarize the support document in concise bullet points. Preserve facts and dates. Do not invent details.")
                .user(text)
                .call()
                .content();
        audit.record("summarize", "document", "success");
        return result;
    }

    public String classify(String ticketId, String message) {
        ensureText(message);
        String result = chatClient.prompt()
                .system("""
                    Classify a customer support ticket.
                    Return exactly these fields as plain text:
                    Category: one of DELIVERY, PAYMENT, PRODUCT, REFUND, OTHER
                    Priority: LOW, MEDIUM, HIGH, URGENT
                    Sentiment: POSITIVE, NEUTRAL, NEGATIVE
                    Reason: one short sentence.
                    Do not output extra fields. Do not invent facts.
                    """)
                .user("Ticket ID: " + ticketId + "\nCustomer message:\n" + message)
                .call()
                .content();
        audit.record("classify", ticketId, "success");
        return result;
    }

    public String answerWithRag(String question) {
        ensureText(question);
        String context = knowledgeBase.retrieve(question);
        String result = chatClient.prompt()
                .system("""
                    You are a customer support assistant. Answer using the supplied policy context.
                    If the context does not answer the question, clearly say that the policy context
                    is insufficient. Do not make up policy details.
                    """)
                .user("Retrieved policy context:\n" + context + "\n\nQuestion:\n" + question)
                .call()
                .content();
        audit.record("rag_answer", "knowledge-base", "success");
        return result;
    }

    public String modelToolCalling(String question) {
        ensureText(question);
        // Spring AI exposes SupportTools to the model. The model may request one of
        // these read-only Java methods; Spring AI executes the requested tool and
        // returns the result to the model so it can formulate a final answer.
        String result = chatClient.prompt()
                .system("""
                    You are a customer-support assistant. Use the available read-only tools
                    when needed to answer questions about an order, payment, or inventory.
                    Do not claim an action was taken. If the order ID or product name is missing,
                    ask a clarifying question instead of guessing.
                    """)
                .user(question)
                .tools(supportTools)
                .call()
                .content();
        audit.record("model_tool_calling", "support-tools", "success");
        return result;
    }

    public String investigate(String orderId, String productName, String customerMessage) {
        ensureText(orderId);
        ensureText(productName);
        ensureText(customerMessage);

        // Java controls mandatory sequence; tool calls are read-only and made explicitly.
        String payment = supportTools.checkPayment(orderId);
        String order = supportTools.checkOrder(orderId);
        String inventory = supportTools.checkInventory(productName);
        String policy = knowledgeBase.retrieve(customerMessage);

        String result = chatClient.prompt()
                .system("""
                    You are a customer-support investigation assistant.
                    Explain findings based only on supplied tool results and policy context.
                    Identify known facts, unknown facts, and safe next steps.
                    Never claim a refund was issued or a delivery date is confirmed unless the
                    supplied data explicitly says so. Do not perform side effects.
                    """)
                .user("""
                    Customer message:
                    %s

                    Payment tool result:
                    %s

                    Order tool result:
                    %s

                    Inventory tool result:
                    %s

                    Retrieved policy:
                    %s

                    Produce: Findings, Unknowns, Recommended next step.
                    """.formatted(customerMessage, payment, order, inventory, policy))
                .call()
                .content();
        audit.record("investigate", orderId, "success");
        return result;
    }

    public String suggestResponse(String ticketId, String customerMessage) {
        ensureText(customerMessage);
        String context = knowledgeBase.retrieve(customerMessage);
        String draft = chatClient.prompt()
                .system("""
                    Draft a short, empathetic customer-support reply. Use only the customer message
                    and policy context. Do not promise a refund, compensation, or delivery date unless
                    confirmed. This is a draft only and must be reviewed by a human before sending.
                    """)
                .user("Ticket: " + ticketId + "\nCustomer message:\n" + customerMessage
                        + "\n\nPolicy context:\n" + context)
                .call()
                .content();
        audit.record("suggest_response", ticketId, "draft_created_pending_approval");
        return draft;
    }

    private void ensureText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Input text must not be blank.");
        }
        if (text.length() > 20000) {
            throw new IllegalArgumentException("Input text exceeds the 20000 character demo limit.");
        }
    }
}
