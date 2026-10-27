package com.acmesupport.observability;

import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Classify the message's category as one of ACCOUNT_ACCESS, BILLING,
            TECHNICAL_ISSUE, FEATURE_REQUEST, or OTHER, and write a short, helpful reply.
            """;

    private final ChatClient chatClient;
    private final SupportMetrics metrics;

    public ChatController(ChatModel chatModel, SupportMetrics metrics) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.metrics = metrics;
    }

    public record ChatRequest(String message) {
    }

    public record ChatResponse(String reply) {
    }

    private record ClassifiedReply(String category, String reply) {
    }

    /**
     * POST /api/chat
     * {"message": "How do I reset my password?"}
     *
     * One model call returns both a category (fed into the support.tickets.classified
     * counter, tagged by category) and the reply. The whole request is timed into
     * support.chat.duration, tagged success/error - check /actuator/metrics after a few
     * calls, or Prometheus/Grafana if you brought up docker-compose.yml.
     */
    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        Timer.Sample sample = metrics.startChatTimer();
        String outcome = "success";
        try {
            ClassifiedReply result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(request.message())
                    .call()
                    .entity(ClassifiedReply.class);

            metrics.recordTicketClassified(result.category());
            return new ChatResponse(result.reply());
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            metrics.stopChatTimer(sample, outcome);
        }
    }
}
