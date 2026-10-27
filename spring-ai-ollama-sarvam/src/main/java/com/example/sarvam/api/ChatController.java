package com.example.sarvam.api;

import com.example.sarvam.service.SarvamChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final SarvamChatService chatService;

    public ChatController(SarvamChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "spring-ai-ollama-sarvam");
    }

    @GetMapping("/ask")
    public ChatResponse askWithQuery(@RequestParam String question) {
        return new ChatResponse(chatService.modelName(), chatService.ask(question));
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.modelName(), chatService.ask(request.question()));
    }

    public record HealthResponse(String status, String application) {
    }
}
