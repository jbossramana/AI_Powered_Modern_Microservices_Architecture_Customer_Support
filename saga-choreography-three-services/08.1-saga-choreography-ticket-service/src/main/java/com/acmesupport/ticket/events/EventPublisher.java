package com.acmesupport.ticket.events;

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
    private final String ticketEventsTopic;
    private final String refundOutcomesTopic;

    public EventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${topics.ticket-events}") String ticketEventsTopic,
            @Value("${topics.refund-outcomes}") String refundOutcomesTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.ticketEventsTopic = ticketEventsTopic;
        this.refundOutcomesTopic = refundOutcomesTopic;
    }

    /**
     * Ticket workflow events go to ticket-events. The final fan-in event goes to
     * refund-outcomes, which is the only topic consumed by communication-service.
     * Wait for broker acknowledgement in this demo so publication failures are not
     * mistaken for successful sends in the application logs.
     */
    public void publish(String key, Object event) {
        String topic = event instanceof TicketEvents.RefundOutcomeReady
                ? refundOutcomesTopic
                : ticketEventsTopic;

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, json).get(10, TimeUnit.SECONDS);
            log.info("Published and broker-acknowledged topic={} key={} event={}",
                    topic, key, event.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing event={} topic={} key={}",
                    event.getClass().getSimpleName(), topic, key, e);
            throw new IllegalStateException("Interrupted while publishing event to " + topic, e);
        } catch (Exception e) {
            log.error("Failed to publish event={} topic={} key={}",
                    event.getClass().getSimpleName(), topic, key, e);
            throw new IllegalStateException(
                    "Failed to publish event " + event.getClass().getSimpleName() + " to topic " + topic, e);
        }
    }
}
