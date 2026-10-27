package com.acmesupport.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The saga's terminal consumer: it only ever listens to refund-outcomes, the one
 * "fan-in" topic both ticket-service's success and failure paths publish to. That's
 * deliberate - this service stays completely insulated from the compensation
 * complexity in ticket-service/billing-service's back-and-forth; it just needs to
 * know the final answer once there is one, which is exactly what a well-chosen
 * "outcome" event buys you in a choreography saga.
 */
@SpringBootApplication
public class CommunicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }
}
