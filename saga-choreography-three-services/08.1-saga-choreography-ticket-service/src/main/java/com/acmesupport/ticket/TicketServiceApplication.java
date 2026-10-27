package com.acmesupport.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ticket-service is both a saga participant AND, in this choreography version, the
 * saga's entry point - there is no separate orchestrator process. It starts the saga
 * by publishing an event after holding the ticket, then reacts to whatever
 * billing-service later publishes about that same sagaId. No @EnableKafka needed:
 * Spring Boot's KafkaAutoConfiguration activates automatically once spring-kafka is on
 * the classpath and wires @KafkaListener support with it.
 */
@SpringBootApplication
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
