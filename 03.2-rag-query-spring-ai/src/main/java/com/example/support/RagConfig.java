package com.example.support;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    CommandLineRunner loadKnowledgeBase(VectorStore vectorStore) {
        return args -> vectorStore.add(List.of(
            new Document(
                """
                Duplicate Payment Policy:
                If a customer reports being charged twice for one order,
                verify the transaction records. If a duplicate charge is
                confirmed, follow the payment team's reversal or refund process.
                """,
                Map.of("category", "payment", "source", "duplicate-payment-policy")
            ),
            new Document(
                """
                Refund Policy:
                Refund eligibility depends on the product, payment status
                and applicable return policy. Verify the order and refund
                status before confirming eligibility or a completion date.
                """,
                Map.of("category", "refund", "source", "refund-policy")
            ),
            new Document(
                """
                Payment Troubleshooting:
                A bank debit may appear before an order payment is confirmed.
                Check the payment gateway transaction status and order records.
                Do not promise a refund until the transaction has been verified.
                """,
                Map.of("category", "troubleshooting", "source", "payment-troubleshooting")
            )
        ));
    }
}
