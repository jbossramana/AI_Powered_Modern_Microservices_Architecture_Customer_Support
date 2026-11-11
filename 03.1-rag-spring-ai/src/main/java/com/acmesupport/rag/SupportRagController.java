package com.acmesupport.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for grounded customer-support answers and retrieval diagnostics.
 * The advisor performs the retrieval used to ground the answer. The additional
 * similarity search supplies source metadata to callers and is useful for learning
 * and troubleshooting; it is not a security boundary.
 */
@RestController
@RequestMapping("/api/support")
public class SupportRagController {

    private final ChatClient ragChatClient;
    private final VectorStore vectorStore;

    public SupportRagController(ChatClient ragChatClient, VectorStore vectorStore) {
        this.ragChatClient = ragChatClient;
        this.vectorStore = vectorStore;
    }

    public record AskRequest(
            @NotBlank(message = "question must not be blank")
            @Size(max = 4000, message = "question must be at most 4000 characters")
            String question) {}

    public record Source(String name, String excerpt, Map<String, Object> metadata) {}
    public record AskResponse(String answer, List<Source> sources, String groundingNote) {}
    public record RetrieveResponse(String query, int resultCount, List<Source> sources) {}

    /**
     * POST /api/support/ask
     * Answer using retrieved knowledge-base context and return likely source chunks.
     */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        String answer = ragChatClient.prompt()
                .user(request.question())
                .call()
                .content();

        List<Source> sources = retrieveSources(request.question());
        String note = sources.isEmpty()
                ? "No matching knowledge-base chunks were found by the diagnostic search. Treat the answer as requiring human verification."
                : "Sources are the top matching knowledge-base chunks. Verify them before using this demo for production decisions.";

        return new AskResponse(answer, sources, note);
    }

    /**
     * POST /api/support/retrieve
     * Inspect retrieval separately from generation to debug chunking and relevance.
     */
    @PostMapping("/retrieve")
    public RetrieveResponse retrieve(@Valid @RequestBody AskRequest request) {
        List<Source> sources = retrieveSources(request.question());
        return new RetrieveResponse(request.question(), sources.size(), sources);
    }

    private List<Source> retrieveSources(String query) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(0.45)
                .build();

        return vectorStore.similaritySearch(searchRequest).stream()
                .map(this::toSource)
                .toList();
    }

    private Source toSource(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object source = metadata.getOrDefault("source", metadata.getOrDefault("file_name", "unknown"));
        String text = document.getText();
        String excerpt = text.length() > 500 ? text.substring(0, 500) + "…" : text;
        return new Source(String.valueOf(source), excerpt, metadata);
    }
}
