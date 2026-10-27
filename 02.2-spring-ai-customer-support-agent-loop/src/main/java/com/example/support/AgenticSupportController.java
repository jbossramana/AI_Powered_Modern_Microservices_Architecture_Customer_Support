package com.example.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support/agent")
public class AgenticSupportController {
    private final AgenticSupportService agenticSupportService;

    public AgenticSupportController(AgenticSupportService agenticSupportService) {
        this.agenticSupportService = agenticSupportService;
    }

    @PostMapping("/ask")
    public Answer ask(@Valid @RequestBody Question request) {
        return new Answer(agenticSupportService.ask(request.question()));
    }

    public record Question(@NotBlank String question) {}
    public record Answer(String answer) {}
}
