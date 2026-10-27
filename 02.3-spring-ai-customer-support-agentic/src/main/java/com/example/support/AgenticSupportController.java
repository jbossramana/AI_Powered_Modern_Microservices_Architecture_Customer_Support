package com.example.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support/agentic")
public class AgenticSupportController {
    private final AgenticSupportService agenticSupportService;

    public AgenticSupportController(AgenticSupportService agenticSupportService) {
        this.agenticSupportService = agenticSupportService;
    }

    @PostMapping("/investigate")
    public Answer investigate(@Valid @RequestBody GoalRequest request) {
        return new Answer(agenticSupportService.investigate(request.goal()));
    }

    public record GoalRequest(@NotBlank String goal) {}
    public record Answer(String answer) {}
}
