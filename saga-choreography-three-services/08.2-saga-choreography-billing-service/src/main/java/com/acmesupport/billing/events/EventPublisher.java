package com.acmesupport.billing.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String billingEventsTopic;

    public EventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${topics.billing-events}") String billingEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.billingEventsTopic = billingEventsTopic;
    }

    public void publish(String key, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(billingEventsTopic, key, json).get(10, TimeUnit.SECONDS);
            log.info("Published and broker-acknowledged topic={} key={} event={}",
                    billingEventsTopic, key, event.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing event={} topic={} key={}",
                    event.getClass().getSimpleName(), billingEventsTopic, key, e);
            throw new IllegalStateException("Interrupted while publishing event to " + billingEventsTopic, e);
        } catch (Exception e) {
            log.error("Failed to publish event={} topic={} key={}",
                    event.getClass().getSimpleName(), billingEventsTopic, key, e);
            throw new IllegalStateException(
                    "Failed to publish event " + event.getClass().getSimpleName()
                            + " to topic " + billingEventsTopic, e);
        }
    }
}
