package com.acmesupport.gateway;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptCacheService {

    private record CacheEntry(String value, Instant expiresAt) {
    }

    private static final Duration TTL = Duration.ofMinutes(10);
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Deliberately keyed on model + prompt only, NOT the caller's API key - identical
     * questions should hit the same cache entry no matter which client asks. Caching
     * (do we already know the answer) and rate limiting (is this caller allowed to ask)
     * are independent concerns and shouldn't share a key.
     */
    public String key(String model, String prompt) {
        return model + "::" + prompt.strip().toLowerCase();
    }

    public Optional<String> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public void put(String key, String value) {
        cache.put(key, new CacheEntry(value, Instant.now().plus(TTL)));
    }
}
