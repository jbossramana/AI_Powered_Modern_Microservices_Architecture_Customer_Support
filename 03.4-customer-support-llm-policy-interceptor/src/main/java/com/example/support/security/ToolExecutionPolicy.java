package com.example.support.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionPolicy {

    private final ObjectMapper objectMapper;

    public ToolExecutionPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(String toolName, String arguments) {
        System.out.println("POLICY CHECK: " + toolName);

        // TEMPORARY TEST RULE: Block order-status tool
		/*
		 * if ("checkOrderStatus".equals(toolName)) { throw new SecurityException(
		 * "Order status tool blocked by policy"); }
		 */

        
        try {
            JsonNode input = objectMapper.readTree(arguments);
            if (input == null || !input.isObject()) {
                throw new SecurityException("Tool arguments must be a JSON object");
            }

            if ("checkPaymentStatus".equals(toolName)
                    || "checkOrderStatus".equals(toolName)) {
                JsonNode orderId = input.get("orderId");

                if (orderId == null || !orderId.isTextual()
                        || !orderId.asText().matches("ORD-[0-9]{3,}")) {
                    throw new SecurityException("Invalid order ID");
                }
                return;
            }

            throw new SecurityException("Tool is not permitted: " + toolName);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException("Invalid tool arguments", ex);
        }
    }
}
