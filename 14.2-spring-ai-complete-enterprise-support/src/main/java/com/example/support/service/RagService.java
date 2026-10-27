package com.example.support.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .build()
        );
    }

    public String contextFor(String question) {
        List<Document> documents = retrieve(question, 4);

        if (documents == null || documents.isEmpty()) {
            return "No relevant knowledge-base passages were found.";
        }

        return documents.stream()
                .map(document -> "[source="
                        + document.getMetadata().getOrDefault("source", "unknown")
                        + "] " + document.getText())
                .collect(Collectors.joining("\n---\n"));
    }

    public List<String> sourceNames(String question) {
        return retrieve(question, 4).stream()
                .map(document -> String.valueOf(
                        document.getMetadata().getOrDefault("source", "unknown")))
                .distinct()
                .toList();
    }
}
