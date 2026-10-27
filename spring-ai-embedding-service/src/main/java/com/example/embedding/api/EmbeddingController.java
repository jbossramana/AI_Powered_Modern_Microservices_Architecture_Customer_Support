package com.example.embedding.api;

import com.example.embedding.service.EmbeddingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * POST /api/embeddings
     * Body: {"text":"I cannot log in to my account"}
     */
    @PostMapping
    public EmbeddingResponse create(@Valid @RequestBody EmbeddingRequest request) {
        try {
            EmbeddingService.EmbeddingResult result =
                    embeddingService.createEmbeddingResult(request.text());

            return new EmbeddingResponse(
                    result.text(),
                    result.dimensions(),
                    result.vector()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    public record EmbeddingRequest(@NotBlank(message = "text must not be blank") String text) {}

    public record EmbeddingResponse(String text, int dimensions, float[] vector) {}
}
