package com.acmesupport.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Step 7: an AI gateway. Every module before this one called Ollama directly. Here,
 * every request is instead a client using one of a small set of provisioned API keys,
 * subject to per-key rate limits, response caching, and centralized usage logging - the
 * same cross-cutting concerns a real gateway (like an internal LiteLLM/Envoy-style
 * proxy) would enforce in front of any model provider, not just Ollama.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
