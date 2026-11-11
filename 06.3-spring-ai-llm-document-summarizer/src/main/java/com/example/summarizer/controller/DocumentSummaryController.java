package com.example.summarizer.controller;

import com.example.summarizer.service.DocumentSummaryService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@RestController
@RequestMapping("/api/documents")
public class DocumentSummaryController {
    private final DocumentSummaryService summaryService;

    public DocumentSummaryController(DocumentSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping(value = "/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> summarize(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a non-empty file.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return ResponseEntity.badRequest().body("Only .txt files are supported.");
        }

        String document = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (document.isBlank()) {
            return ResponseEntity.badRequest().body("The uploaded file contains no text.");
        }

        String summary = summaryService.summarize(document);
        return ResponseEntity.ok(summary);
    }
}
