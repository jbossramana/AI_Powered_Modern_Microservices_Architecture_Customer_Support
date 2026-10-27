package com.example.support;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportRagService ragService;

    public SupportController(SupportRagService ragService) {
        this.ragService = ragService;
    }

    public record QuestionRequest(String question) {}

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody QuestionRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be empty");
        }
        return ragService.answer(request.question().trim());
    }
}
