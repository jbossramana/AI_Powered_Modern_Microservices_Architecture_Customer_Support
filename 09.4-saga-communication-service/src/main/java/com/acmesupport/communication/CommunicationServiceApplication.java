package com.acmesupport.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The one service in this saga that isn't part of the compensatable chain - it's
 * called unconditionally by the orchestrator once the saga has resolved one way or
 * the other (approved, after ticket-hold + refund succeeded; or declined, after
 * whatever compensation was needed). Drafting a message has no side effect worth
 * undoing, so there's no compensating action here - just a forward action, run once,
 * telling the customer what actually happened.
 */
@SpringBootApplication
public class CommunicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationServiceApplication.class, args);
    }
}
