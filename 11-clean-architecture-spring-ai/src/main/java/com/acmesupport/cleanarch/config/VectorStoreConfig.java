package com.acmesupport.cleanarch.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.List;

/**
 * Choosing an in-memory SimpleVectorStore (vs. pgvector, Redis, etc.) is an
 * infrastructure decision, same category as choosing Spring Boot and Ollama in the
 * first place - it belongs in this outermost layer. Nothing inside application/ or
 * domain/ knows this choice was made; they only see the KnowledgeBaseSearchPort
 * interface, implemented by adapter/out/ai/SpringAiKnowledgeBaseSearchAdapter, which
 * depends on VectorStore as an abstraction rather than on SimpleVectorStore specifically.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    ApplicationRunner ingestKnowledgeBase(VectorStore vectorStore, ResourcePatternResolver resourcePatternResolver) {
        return (ApplicationArguments args) -> {
            Resource[] articles = resourcePatternResolver.getResources("classpath:knowledge-base/*.md");
            TokenTextSplitter splitter = new TokenTextSplitter();
            for (Resource article : articles) {
                TextReader reader = new TextReader(article);
                reader.getCustomMetadata().put("source", article.getFilename());
                List<Document> chunks = splitter.apply(reader.get());
                vectorStore.add(chunks);
            }
        };
    }
}
