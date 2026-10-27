package com.acmesupport.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Three named RestClient beans, one per saga participant, each pointed at that
 * service's base URL from application.yml. RestClient.Builder is autoconfigured as a
 * prototype bean, so each @Bean method here gets its own independent builder instance
 * to customize - no risk of one client's baseUrl leaking into another's.
 *
 * <p>Real deployments would typically add service discovery (Kubernetes DNS, Eureka)
 * instead of hardcoded URLs, and often put a message broker between the orchestrator
 * and participants instead of synchronous REST, for durability if a participant is
 * temporarily down. Kept as plain, synchronous HTTP calls here so the saga's control
 * flow in RefundSagaOrchestrator is easy to read start to finish without an extra
 * layer of messaging infrastructure to reason about at the same time.
 */
@Configuration
public class RestClientConfig {

    @Bean
    RestClient ticketServiceClient(RestClient.Builder builder, @Value("${services.ticket.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    RestClient billingServiceClient(RestClient.Builder builder, @Value("${services.billing.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    RestClient communicationServiceClient(RestClient.Builder builder, @Value("${services.communication.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
