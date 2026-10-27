package com.acmesupport.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Spring AI already instruments every ChatModel/EmbeddingModel call automatically once
 * actuator + a Micrometer tracing bridge are on the classpath - latency, errors, and
 * (where the provider reports it) token usage, with zero extra code. This class is the
 * layer above that baseline: metrics that mean something specific to THIS business -
 * how many messages of each category came through, how long a full support turn takes
 * end-to-end - which only the application itself knows how to record.
 */
@Component
public class SupportMetrics {

    private final MeterRegistry registry;

    public SupportMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTicketClassified(String category) {
        Counter.builder("support.tickets.classified")
                .description("Support messages classified by category")
                .tag("category", category)
                .register(registry)
                .increment();
    }

    public Timer.Sample startChatTimer() {
        return Timer.start(registry);
    }

    public void stopChatTimer(Timer.Sample sample, String outcome) {
        sample.stop(Timer.builder("support.chat.duration")
                .description("End-to-end duration of a support chat request, from receipt to reply")
                .tag("outcome", outcome)
                .register(registry));
    }
}
