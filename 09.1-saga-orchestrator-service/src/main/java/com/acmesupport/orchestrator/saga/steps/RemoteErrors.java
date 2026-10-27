package com.acmesupport.orchestrator.saga.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientResponseException;

/**
 * Every participant in this series returns errors as {@code {"error": "..."}} (see
 * ApiExceptionHandler-style handlers throughout). This translates that into a plain
 * String instead of letting raw HTTP status text or JSON braces leak into what
 * eventually becomes an LLM-drafted, customer-facing message.
 */
final class RemoteErrors {

    private RemoteErrors() {
    }

    static String extract(RestClientResponseException e, ObjectMapper objectMapper, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(e.getResponseBodyAsString());
            String error = node.path("error").asText(null);
            return error != null ? error : fallback;
        } catch (Exception parseFailure) {
            return fallback;
        }
    }
}
