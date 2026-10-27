package com.acmesupport.cleanarch.adapter.out.ai;

import com.acmesupport.cleanarch.application.port.out.KnowledgeBaseSearchPort;
import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Translates a VectorStore similarity search (Spring AI's world - Document, metadata
 * maps, similarity thresholds) into the plain KnowledgeArticle list the application
 * layer asked for via KnowledgeBaseSearchPort. The VectorStore bean itself is defined
 * in config/VectorStoreConfig - choosing SimpleVectorStore vs. pgvector vs. anything
 * else never needs to touch this class.
 */
@Component
public class SpringAiKnowledgeBaseSearchAdapter implements KnowledgeBaseSearchPort {

    private final VectorStore vectorStore;

    public SpringAiKnowledgeBaseSearchAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public List<KnowledgeArticle> search(String query, int topK) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).similarityThreshold(0.45).build());

        return results.stream()
                .map(doc -> new KnowledgeArticle(
                        String.valueOf(doc.getMetadata().getOrDefault("source", "Untitled")),
                        doc.getText()))
                .toList();
    }
}
