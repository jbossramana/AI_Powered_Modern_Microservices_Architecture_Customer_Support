package com.acmesupport.mlclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MlServiceConfig {

	 @Bean
	    public RestClient.Builder restClientBuilder() {
	        return RestClient.builder();
	    }
	 
    @Bean
    RestClient mlServiceClient(RestClient.Builder builder, @Value("${services.ml-priority.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
