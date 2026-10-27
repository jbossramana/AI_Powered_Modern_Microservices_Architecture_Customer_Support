package com.example.embedding.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Converts text into an embedding vector using the configured Spring AI model.
 *
 * This service does not store vectors or calculate similarity; those are separate
 * responsibilities commonly handled by a vector store/search service.
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Returns the embedding vector for the supplied text.
     */
    public float[] createEmbedding(String text) {
        validateText(text);
        return embeddingModel.embed(text.trim());
    }

    /**
     * Returns useful metadata without returning the full vector.
     */
    public EmbeddingResult createEmbeddingResult(String text) {
        float[] vector = createEmbedding(text);
        return new EmbeddingResult(text.trim(), vector.length, vector);
    }

    private void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text must not be null or blank.");
        }
    }

    public record EmbeddingResult(String text, int dimensions, float[] vector) {
        public EmbeddingResult {
            vector = Arrays.copyOf(vector, vector.length);
        }

        @Override
        public float[] vector() {
            return Arrays.copyOf(vector, vector.length);
        }
    }
}
