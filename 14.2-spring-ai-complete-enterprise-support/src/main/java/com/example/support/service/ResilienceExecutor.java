package com.example.support.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

@Service
public class ResilienceExecutor {

    private static final int MAX_ATTEMPTS = 3;
    private static final long CALLER_TIMEOUT_SECONDS = 35;
    private static final long CIRCUIT_OPEN_MILLIS = 20_000;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    private volatile long circuitOpenUntilMillis;

    public <T> T execute(Callable<T> operation) {
        if (System.currentTimeMillis() < circuitOpenUntilMillis) {
            throw new IllegalStateException("AI circuit is open. Retry after a short pause.");
        }

        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Future<T> future = executor.submit(operation);

            try {
                T result = future.get(CALLER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                consecutiveFailures.set(0);
                return result;
            } catch (TimeoutException exception) {
                future.cancel(true);
                lastFailure = exception;
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                lastFailure = cause instanceof Exception e
                        ? e
                        : new RuntimeException(cause);
            } catch (InterruptedException exception) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("AI request was interrupted.", exception);
            }

            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Retry wait was interrupted.", exception);
                }
            }
        }

        if (consecutiveFailures.incrementAndGet() >= 3) {
            circuitOpenUntilMillis = System.currentTimeMillis() + CIRCUIT_OPEN_MILLIS;
        }

        throw new IllegalStateException("AI request failed after retries.", lastFailure);
    }
}
