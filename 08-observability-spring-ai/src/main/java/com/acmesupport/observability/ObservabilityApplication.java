package com.acmesupport.observability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 8: observability. With spring-boot-starter-actuator and a Micrometer tracing
 * bridge on the classpath, Spring AI automatically records an Observation for every
 * ChatModel/EmbeddingModel call - request/response metadata, token usage where the
 * provider reports it, latency, errors - with zero code changes anywhere else in this
 * module. What IS written here is the layer above that baseline: business-specific
 * metrics that mean something to this particular app (SupportMetrics), not just
 * "a model was called".
 */
@SpringBootApplication
public class ObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityApplication.class, args);
    }
}
