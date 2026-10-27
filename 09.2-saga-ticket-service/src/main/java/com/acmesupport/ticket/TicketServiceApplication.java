package com.acmesupport.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One of three saga participants (alongside billing-service and communication-service),
 * coordinated by saga-orchestrator-service. This service knows nothing about sagas,
 * orchestration, or the other services - it just exposes a small, honest local API
 * (hold a ticket for refund, release that hold, mark resolved) and lets its own local
 * database transaction be the unit of consistency. That's the whole point of the
 * pattern: no service here needs to know it's part of a larger distributed transaction.
 */
@SpringBootApplication
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
