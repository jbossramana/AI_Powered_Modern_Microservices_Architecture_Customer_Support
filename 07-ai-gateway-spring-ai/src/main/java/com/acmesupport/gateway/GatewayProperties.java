package com.acmesupport.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(Map<String, ApiKeyConfig> apiKeys) {

    public record ApiKeyConfig(String clientName, int requestsPerMinute) {
    }
}
