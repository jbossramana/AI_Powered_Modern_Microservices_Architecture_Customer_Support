package com.acmesupport.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The saga participant with real money on the line, which is exactly why sagas exist:
 * you can't wrap a call to a payment/refund system and a call to your own ticket
 * database in one ACID transaction across process and database boundaries. Instead,
 * this service commits its own local transaction (process the refund) independently,
 * and exposes a compensating action (reverse it) for the orchestrator to call if a
 * later step in the saga fails.
 */
@SpringBootApplication
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
