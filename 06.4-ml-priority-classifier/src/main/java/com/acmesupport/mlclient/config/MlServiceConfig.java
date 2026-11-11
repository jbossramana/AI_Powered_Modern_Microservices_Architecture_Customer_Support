package com.acmesupport.mlclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MlServiceConfig {

    @Bean("mlServiceClient")
    public RestClient mlServiceClient(
            @Value("${ml.priority.base-url:http://localhost:8000}")
            String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}