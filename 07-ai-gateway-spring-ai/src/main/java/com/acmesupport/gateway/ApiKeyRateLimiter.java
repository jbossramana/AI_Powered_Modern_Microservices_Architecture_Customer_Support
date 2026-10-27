package com.acmesupport.gateway;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A hand-rolled token bucket, one per API key, refilling continuously. Good enough for
 * a single gateway instance; a multi-instance deployment would centralize this same
 * algorithm's state in Redis (e.g. via a Lua script for atomicity) so the limit is
 * enforced across all instances rather than per-instance.
 */
@Component
public class ApiKeyRateLimiter {

    private static final class Bucket {
        final double capacity;
        final double refillPerNano;
        double tokens;
        long lastRefillNanos;

        Bucket(double capacity, double refillPerNano) {
            this.capacity = capacity;
            this.refillPerNano = refillPerNano;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - lastRefillNanos) * refillPerNano);
            lastRefillNanos = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String apiKey, int requestsPerMinute) {
        Bucket bucket = buckets.computeIfAbsent(apiKey,
                k -> new Bucket(requestsPerMinute, requestsPerMinute / 60.0 / 1_000_000_000.0));
        return bucket.tryConsume();
    }
}
