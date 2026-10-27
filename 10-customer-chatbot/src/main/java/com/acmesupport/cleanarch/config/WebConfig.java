package com.acmesupport.cleanarch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * DEV-ONLY CORS setup so support-chatbot.html can call this API when opened directly
 * as a local file (browsers send an opaque/"null" origin for file://) or served from a
 * different host/port than this backend. Belongs in config/ alongside UseCaseConfig
 * and VectorStoreConfig - like them, it's an outermost-layer concern that domain/ and
 * application/ never need to know about.
 *
 * <p>In production, replace allowedOriginPatterns("*") with your actual frontend's
 * real origin(s) - a wildcard origin on a customer-facing API is not a setting you
 * want to ship.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
