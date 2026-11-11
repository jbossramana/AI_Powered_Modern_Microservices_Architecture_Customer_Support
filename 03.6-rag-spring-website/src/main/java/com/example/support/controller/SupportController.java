package com.example.support.controller;

import com.example.support.service.CustomerSupportService;
import com.example.support.service.WebsiteIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final CustomerSupportService supportService;
    private final WebsiteIngestionService ingestionService;
    private final List<String> configuredUrls;

    public SupportController(
            CustomerSupportService supportService,
            WebsiteIngestionService ingestionService,
            @Value("${app.website.urls}") List<String> configuredUrls) {
        this.supportService = supportService;
        this.ingestionService = ingestionService;
        this.configuredUrls = configuredUrls;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "customer-support-rag");
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }
        return Map.of("question", question, "answer", supportService.answer(question));
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingestConfiguredPages() throws IOException {
        try {
            int chunks = ingestionService.indexPages(configuredUrls);
            return Map.of(
                    "status", "completed",
                    "pagesRequested", configuredUrls.size(),
                    "chunksIndexed", chunks,
                    "urls", configuredUrls
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Website ingestion failed: " + ex.getMessage(),
                    ex
            );
        }
    }
}
