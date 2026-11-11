
package com.example.ragcache;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;

import jakarta.annotation.PostConstruct;

@Service
public class RagService {

    // Logger used to monitor cache behavior, document indexing,
    // and calls to the RAG + LLM processing pipeline.
    private static final Logger log =
            LoggerFactory.getLogger(RagService.class);

    // ChatClient is Spring AI's high-level API for interacting
    // with the configured chat model, such as Ollama's llama3.2.
    private final ChatClient chatClient;

    // VectorStore stores document embeddings and associated metadata.
    // It searches for relevant documents when a question is asked.
    private final VectorStore vectorStore;

    // CacheManager provides access to the configured Spring caches,
    // including the "ragAnswers" cache.
    private final CacheManager cacheManager;

    // Constructor injection: Spring supplies the ChatClient builder,
    // VectorStore bean, and CacheManager bean.
    public RagService(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            CacheManager cacheManager) {

        // Build a ChatClient using the application's configured model.
        this.chatClient = builder.build();

        this.vectorStore = vectorStore;
        this.cacheManager = cacheManager;
    }

    // Runs once after Spring has constructed this service bean.
    // These sample documents are embedded and added to the vector store
    // so the RAG application has policy information to retrieve.
    @PostConstruct
    void loadStarterDocuments() {

        List<Document> docs = List.of(

            // Document 1: policy for duplicate payments.
            // The content is searchable through its generated embedding.
            // Metadata provides additional information about the document.
            new Document(
                "If a customer is charged more than once for the same order, "
              + "the duplicate transaction must first be verified against "
              + "payment records. Once verified and approved, the duplicate "
              + "payment is eligible for a refund.",
                Map.of(
                    "title", "Duplicate payment refund policy",
                    "category", "refund",
                    "version", "1"
                )
            ),

            // Document 2: expected refund processing time.
            new Document(
                "After a refund is approved, it normally takes 5 to 7 "
              + "business days for the refund to appear, depending on "
              + "the payment provider and bank.",
                Map.of(
                    "title", "Refund processing time",
                    "category", "refund",
                    "version", "1"
                )
            ),

            // Document 3: payment verification and support procedure.
            new Document(
                "If payment status is unclear, support staff must verify "
              + "the transaction using the authorized payment system before "
              + "confirming a duplicate charge. Never claim a refund has "
              + "been issued unless the payment system confirms it.",
                Map.of(
                    "title", "Payment verification procedure",
                    "category", "support",
                    "version", "1"
                )
            )
        );

        // Add all three documents to the vector store.
        // The vector store uses the configured embedding model to create
        // embeddings, enabling similarity-based retrieval later.
        vectorStore.add(docs);

        log.info(
            "Loaded {} starter documents into the in-memory vector store",
            docs.size()
        );
    }

    // Cache the final answer for each normalized question.
    //
    // cacheNames = "ragAnswers":
    //     Selects the cache in which the generated answer is stored.
    //
    // key:
    //     Converts the question to lowercase and removes leading/trailing
    //     whitespace, so simple case and whitespace variations can reuse
    //     the same cache entry.
    //
    // IMPORTANT:
    // Spring's caching interceptor checks the cache before executing
    // this method. On a cache HIT, this method body is skipped.
    // On a cache MISS, the method executes and its returned answer is cached.
    @Cacheable(
        cacheNames = "ragAnswers",
        key = "#question.toLowerCase(T(java.util.Locale).ROOT).trim()"
    )
    public String answer(String question) {

        // This log line is reached only when the cache does not supply
        // an existing answer, assuming caching is enabled and intercepted.
        log.info(
            "CACHE MISS: running retrieval and LLM for question='{}'",
            question
        );

        // Build the prompt sent to the chat model.
        String answer = chatClient.prompt()

            // System instructions guide the LLM to answer from retrieved
            // policy content and avoid inventing unsupported information.
            .system(
                "You are a customer-support assistant. "
              + "Use the retrieved policy context. "
              + "If the context is insufficient, say so. "
              + "Do not invent policy details. "
              + "Do not claim a refund has been issued."
            )

            // The user's question becomes the query for the RAG pipeline.
            .user(question)

            // QuestionAnswerAdvisor connects retrieval to the chat request.
            // It searches the vector store and adds relevant document content
            // to the context supplied to the LLM.
            .advisors(
                QuestionAnswerAdvisor.builder(vectorStore)

                    // Retrieve up to three relevant document matches.
                    // topK(3) is the maximum number requested, not a guarantee
                    // that three documents will always be found.
                    .searchRequest(
                        SearchRequest.builder()
                            .topK(3)
                            .build()
                    )
                    .build()
            )

            // Execute the request against the configured chat model.
            .call()

            // Extract the generated response as a String.
            .content();

        // Return a fallback message if the model provides no content.
        // Otherwise, return the generated answer to the caller.
        // With @Cacheable, this returned String is cached on a cache MISS.
        return answer == null
                ? "The model returned an empty answer."
                : answer;
    }

    // Add a new document to the vector store at runtime.
    public void addDocument(String title, String content) {

        // Create a Document containing the supplied content and metadata.
        // vectorStore.add(...) indexes the document for later retrieval.
        vectorStore.add(
            List.of(
                new Document(
                    content,
                    Map.of(
                        "title", title,
                        "category", "user-added",
                        "version", "1"
                    )
                )
            )
        );

        // Previously cached answers may be stale after document changes.
        // Clear the answer cache so subsequent questions run through RAG
        // and can retrieve the newly indexed content.
        clearAnswerCache();

        log.info(
            "Indexed '{}' and invalidated answer cache",
            title
        );
    }

    // Return information about the configured answer cache.
    // A controller can expose this data through a cache-stats endpoint.
    public Map<String, Object> cacheStats() {

        // Get the Spring cache named "ragAnswers".
        org.springframework.cache.Cache springCache =
                cacheManager.getCache("ragAnswers");

        // If the cache is not configured or registered, report unavailable.
        if (springCache == null) {
            return Map.of("available", false);
        }

        // Obtain the underlying cache implementation.
        // This project uses Caffeine, so we can access its statistics.
        Object nativeCache = springCache.getNativeCache();

        // Check whether the native cache is a Caffeine cache.
        if (nativeCache instanceof Cache<?, ?> caffeine) {

            // Read hit, miss, hit-rate, size, and eviction statistics.
            var stats = caffeine.stats();

            return Map.of(
                "available", true,
                "estimatedSize", caffeine.estimatedSize(),
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", stats.hitRate(),
                "evictionCount", stats.evictionCount()
            );
        }

        // If another cache provider is configured, return its class name
        // rather than trying to access Caffeine-specific statistics.
        return Map.of(
            "available", true,
            "provider", nativeCache.getClass().getName()
        );
    }

    // Remove all entries from the answer cache.
    // This does NOT delete documents from the vector store.
    public void clearAnswerCache() {

        org.springframework.cache.Cache cache =
                cacheManager.getCache("ragAnswers");

        // Clear the cache only when it exists.
        if (cache != null) {
            cache.clear();
        }
    }
}
