package com.example.ragcache;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(value="/ask", consumes=MediaType.APPLICATION_JSON_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        long started = System.nanoTime();
        String answer = ragService.answer(request.question().trim());
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        return new AskResponse(request.question(), answer, elapsedMs, ragService.cacheStats());
    }

    @PostMapping(value="/documents", consumes=MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> addDocument(@Valid @RequestBody AddDocumentRequest request) {
        ragService.addDocument(request.title().trim(), request.content().trim());
        return Map.of("status", "indexed", "title", request.title(),
                "message", "Added to the in-memory demo vector store; restart clears this index.");
    }

    @GetMapping("/cache/stats")
    public Map<String, Object> cacheStats() {
        return ragService.cacheStats();
    }

    @DeleteMapping("/cache")
    public Map<String, String> clearCache() {
        ragService.clearAnswerCache();
        return Map.of("status", "cleared", "cache", "ragAnswers");
    }

    public record AskRequest(@NotBlank String question) {}
    public record AddDocumentRequest(@NotBlank String title, @NotBlank String content) {}
    public record AskResponse(String question, String answer, long elapsedMs,
                              Map<String, Object> cacheStats) {}
}
