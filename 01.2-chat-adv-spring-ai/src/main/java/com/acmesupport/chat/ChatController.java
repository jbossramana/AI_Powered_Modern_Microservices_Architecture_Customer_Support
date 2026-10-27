package com.acmesupport.chat;

import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient supportChatClient) {
        this.chatClient = supportChatClient;
    }

    public record ChatRequest(@NotBlank String message) {
    }

    public record ChatResponse(String reply) {
    }

    /**
     * POST /api/chat
     * {"message": "How do I reset my password?"}
     */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatClient.prompt()
                .user(request.message())
                .call()
                .content();
        return new ChatResponse(reply);
    }
}
