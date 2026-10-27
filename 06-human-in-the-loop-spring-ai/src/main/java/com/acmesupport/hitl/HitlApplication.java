package com.acmesupport.hitl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 6: human-in-the-loop approval. Every module so far returned an AI-generated
 * answer directly to the caller. This one deliberately does not: drafting a response
 * and sending it to a customer are split into two separate steps, with a human
 * required in between. Nothing gets "sent" without an explicit approve call on an
 * item whose status is APPROVED.
 */
@SpringBootApplication
public class HitlApplication {

    public static void main(String[] args) {
        SpringApplication.run(HitlApplication.class, args);
    }
}
