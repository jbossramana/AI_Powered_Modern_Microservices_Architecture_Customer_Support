package com.example.support;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportRagService supportRagService;

    public SupportController(SupportRagService supportRagService) {
        this.supportRagService = supportRagService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(
            @RequestBody AskRequest request) {

        if (request == null || request.question() == null
                || request.question().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", "question must not be empty"));
        }

        return ResponseEntity.ok(
                supportRagService.answer(request.question()));
    }

    public record AskRequest(String question) {
    }
}
