package com.example.support.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.List;

@Configuration
class KnowledgeBaseConfiguration {
    @Bean
    VectorStore supportVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(List.of(
                new Document("""
                        Delivery delays: First verify payment and shipment status.
                        Apologize for the delay, provide only a confirmed delivery estimate,
                        and escalate if tracking has not updated for 48 hours.
                        """, java.util.Map.of("topic", "delivery")),
                new Document("""
                        Refund policy: Support agents must verify order and payment details.
                        Never promise a refund before eligibility is confirmed. Refund requests
                        require the normal approval workflow.
                        """, java.util.Map.of("topic", "refund")),
                new Document("""
                        Payment troubleshooting: A successful payment does not guarantee that
                        a shipment has been dispatched. Check the order and shipment separately.
                        Never ask customers to share full card numbers, passwords, or one-time codes.
                        """, java.util.Map.of("topic", "payment")),
                new Document("""
                        Communication policy: Be empathetic, concise, and factual. Do not invent
                        order status, delivery dates, policy terms, or refunds. If information is
                        unavailable, say that it needs to be checked.
                        """, java.util.Map.of("topic", "communication"))
        ));
        return store;
    }
}

@Service
public class KnowledgeBaseService {
    private final VectorStore vectorStore;

    public KnowledgeBaseService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String retrieve(String question) {
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(3).build());
        if (matches == null || matches.isEmpty()) {
            return "No relevant policy passages were found.";
        }
        return matches.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .reduce((a, b) -> a + "\n\n---\n\n" + b)
                .orElse("No relevant policy passages were found.");
    }
}
