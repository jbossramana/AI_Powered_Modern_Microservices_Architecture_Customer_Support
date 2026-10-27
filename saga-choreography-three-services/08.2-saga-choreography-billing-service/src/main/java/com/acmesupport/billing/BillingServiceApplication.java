package com.acmesupport.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The saga participant with real money on the line - unchanged in principle from the
 * orchestration version, only the trigger changed: instead of a REST call from an
 * orchestrator telling it "process this refund now," it reacts to a TicketRefundHeld
 * event arriving on a topic it subscribes to. It has no idea ticket-service exists as
 * a specific service, only that "something" publishes events shaped like
 * TicketRefundHeld to the ticket-events topic.
 */
@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
