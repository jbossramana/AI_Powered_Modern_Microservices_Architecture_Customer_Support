package com.example.support;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        store.add(List.of(
                new Document(
                        "Duplicate payment policy: If a customer believes they were charged twice, "
                        + "support should verify both transaction records before confirming a duplicate charge. "
                        + "Do not promise a refund or completion date until billing confirms eligibility.",
                        Map.of("source", "duplicate-payment-policy")),
                new Document(
                        "Refund policy: Once a duplicate payment is confirmed by the billing team, "
                        + "an eligible refund is submitted according to the company's payment process. "
                        + "The exact processing time depends on payment method and must be confirmed by billing.",
                        Map.of("source", "refund-policy")),
                new Document(
                        "Order support policy: Ask the customer for the order ID when checking order details. "
                        + "Order status alone does not prove whether a payment was duplicated.",
                        Map.of("source", "order-support-policy")),
                new Document(
                        "Ticket support policy: Use the ticket ID to check the current support ticket status. "
                        + "If a ticket is in progress, do not describe the issue as resolved or promise a resolution date.",
                        Map.of("source", "ticket-support-policy"))
        ));

        return store;
    }
}
