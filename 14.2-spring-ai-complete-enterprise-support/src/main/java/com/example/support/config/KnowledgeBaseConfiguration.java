package com.example.support.config;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeBaseConfiguration {

    @Bean
    ApplicationRunner seedKnowledgeBase(VectorStore vectorStore) {
        return args -> vectorStore.add(List.of(
                document(
                        "Ticket triage: classify a support ticket by category, urgency, and sentiment. "
                                + "Summarize the customer's issue and propose next steps. Escalate safety, "
                                + "security, legal, and high-impact payment issues.",
                        "support-triage.md", "manual"
                ),
                document(
                        "Refund policy: verify the order and payment status. Refunds require eligibility "
                                + "checks and human approval. Never promise a refund before approval.",
                        "refund-policy.md", "policy"
                ),
                document(
                        "Shipping FAQ: check the latest order status and carrier event. Estimated delivery "
                                + "dates can change. Explain verified facts and avoid inventing an ETA.",
                        "shipping-faq.md", "faq"
                ),
                document(
                        "Payment troubleshooting: check payment state using the payment system. Never ask "
                                + "for passwords, one-time passcodes, or full card numbers. Escalate duplicate "
                                + "charges or uncertain payment state.",
                        "payment-troubleshooting.md", "troubleshooting"
                ),
                document(
                        "Warranty FAQ: warranty eligibility depends on product, purchase date, and policy. "
                                + "Check the customer and order record before making eligibility claims.",
                        "warranty-faq.md", "faq"
                ),
                document(
                        "SLA policy: classify severity using business impact and urgency. High-impact "
                                + "service outages and security incidents must be escalated to a human team.",
                        "sla-policy.md", "policy"
                ),
                document(
                        "Known issue KB-17: some tracking events may arrive late. Check the most recent "
                                + "carrier event before concluding that a package has stopped moving.",
                        "known-issues.md", "known-issue"
                ),
                document(
                        "Release notes: the support portal now supports ticket summaries and suggested "
                                + "responses. AI-generated replies must be reviewed by an agent before sending.",
                        "release-notes.md", "release-note"
                )
        ));
    }

    private static Document document(String text, String source, String type) {
        return new Document(text, Map.of("source", source, "type", type, "version", "1"));
    }
}
