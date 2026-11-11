package com.example.support.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class PaymentTools {

    @Tool(description = "Check payment status for an order ID. Input must include orderId.")
    public String checkPaymentStatus(String orderId) {
        System.out.println("Executing payment tool: " + orderId);

        if ("ORD-101".equals(orderId)) {
            return "Payment status: SUCCESS";
        }
        return "Payment status: NOT_FOUND";
    }
}
