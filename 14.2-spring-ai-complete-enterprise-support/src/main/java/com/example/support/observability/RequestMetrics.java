package com.example.support.observability;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class RequestMetrics {

    private final AtomicLong requests = new AtomicLong();

    public RequestMetrics(MeterRegistry registry) {
        registry.gauge("support.requests.started", requests);
    }

    public void markRequestStarted() {
        requests.incrementAndGet();
    }
}
