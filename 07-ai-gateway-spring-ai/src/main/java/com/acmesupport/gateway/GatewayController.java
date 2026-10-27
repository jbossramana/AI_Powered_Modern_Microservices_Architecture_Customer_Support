package com.acmesupport.gateway;

import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/gateway/v1")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final ChatClient chatClient;
    private final ApiKeyRateLimiter rateLimiter;
    private final PromptCacheService cache;
    private final GatewayProperties properties;
    private final String modelName;

    public GatewayController(
            ChatModel chatModel,
            ApiKeyRateLimiter rateLimiter,
            PromptCacheService cache,
            GatewayProperties properties,
            @Value("${spring.ai.ollama.chat.options.model}") String modelName) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.rateLimiter = rateLimiter;
        this.cache = cache;
        this.properties = properties;
        this.modelName = modelName;
    }

    public record ChatRequest(@NotBlank String message) {
    }

    /**
     * POST /gateway/v1/chat, header X-API-Key: demo-internal-portal
     * {"message": "..."}
     *
     * Every request passes through, in order: (1) API key check, (2) rate limit check,
     * (3) cache lookup, (4) the actual model call, (5) usage logging. Steps 1-3 can all
     * short-circuit before Ollama is ever called.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody ChatRequest request) {

        long startedAt = System.currentTimeMillis();

        if (apiKey == null || !properties.apiKeys().containsKey(apiKey)) {
            log.warn("Rejected request: missing or unrecognized API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid_api_key",
                    "message", "Missing or unrecognized X-API-Key header."));
        }

        GatewayProperties.ApiKeyConfig config = properties.apiKeys().get(apiKey);

        if (!rateLimiter.tryConsume(apiKey, config.requestsPerMinute())) {
            log.warn("Rate limit exceeded client={}", config.clientName());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "rate_limit_exceeded",
                    "message", "Limit is %d requests/minute for this key. Slow down and retry shortly."
                            .formatted(config.requestsPerMinute())));
        }

        String cacheKey = cache.key(modelName, request.message());
        Optional<String> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            log.info("client={} cached=true latencyMs={}", config.clientName(), System.currentTimeMillis() - startedAt);
            return ResponseEntity.ok(Map.of("reply", cached.get(), "cached", true, "model", modelName));
        }

        String reply = chatClient.prompt().user(request.message()).call().content();
        cache.put(cacheKey, reply);

        long latencyMs = System.currentTimeMillis() - startedAt;
        // Centralized usage logging: in a real gateway this feeds metering/billing and
        // the observability pipeline (Module 8) instead of a plain log line - but every
        // request crossing this exact point is what makes that possible at all.
        log.info("client={} model={} cached=false latencyMs={} promptChars={} replyChars={}",
                config.clientName(), modelName, latencyMs, request.message().length(), reply.length());

        return ResponseEntity.ok(Map.of("reply", reply, "cached", false, "model", modelName));
    }
}
