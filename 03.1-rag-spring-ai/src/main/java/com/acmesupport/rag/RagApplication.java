package com.acmesupport.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 3: Retrieval-Augmented Generation. Five knowledge-base articles are embedded
 * into an in-memory vector store on startup; every chat request is now grounded in
 * whichever articles are semantically closest to the question, instead of only the
 * model's own (often outdated, sometimes invented) idea of your product's policies.
 */
@SpringBootApplication
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
