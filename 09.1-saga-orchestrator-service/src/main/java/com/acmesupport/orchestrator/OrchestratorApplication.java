package com.acmesupport.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The orchestrator: a single place that knows the whole "process a refund" business
 * transaction's shape - hold ticket, process refund, confirm resolved, compensating
 * in reverse on failure, then always drafting and delivering an outcome message. No
 * participant service (ticket-service, billing-service, communication-service) knows
 * this sequence exists; that knowledge is centralized here. That centralization is
 * the defining trait of an ORCHESTRATION saga, as opposed to a CHOREOGRAPHY saga where
 * each service reacts to events from the last one and no single place holds the whole
 * flow - see the top-level README for that comparison.
 */
@SpringBootApplication
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
    
    
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
