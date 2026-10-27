package com.example.support.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {

    @Tool(description = "Check order creation and fulfillment status. Input must include orderId.")
    public String checkOrderStatus(String orderId) {
        System.out.println("Executing order tool: " + orderId);

        if ("ORD-101".equals(orderId)) {
            return "Order status: NOT_CREATED";
        }
        return "Order status: CREATED";
    }
}
