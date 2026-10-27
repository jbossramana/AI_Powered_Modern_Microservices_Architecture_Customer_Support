package com.example.nativetools.tools;

import com.example.nativetools.data.MockOrderRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class OrderInvestigationTools {
    private final MockOrderRepository repository;
    public OrderInvestigationTools(MockOrderRepository repository) { this.repository = repository; }

    @Tool(description = "Check payment status, paid amount, and transaction ID for an order. Read-only. Use when payment facts are needed.")
    public MockOrderRepository.Payment checkPayment(String orderId) {
        System.out.println("\n[NATIVE TOOL CALL] checkPayment(orderId=" + orderId + ")");
        var result = repository.payment(orderId);
        System.out.println("[TOOL RESULT] " + result);
        System.out.println("[TOOL RETURN] Spring AI returns this result to the LLM.");
        return result;
    }

    @Tool(description = "Check order status, shipment status, and tracking ID for an order. Read-only.")
    public MockOrderRepository.Order checkOrder(String orderId) {
        System.out.println("\n[NATIVE TOOL CALL] checkOrder(orderId=" + orderId + ")");
        var result = repository.order(orderId);
        System.out.println("[TOOL RESULT] " + result);
        System.out.println("[TOOL RETURN] Spring AI returns this result to the LLM.");
        return result;
    }

    @Tool(description = "Check inventory availability and quantity for an order. Read-only.")
    public MockOrderRepository.Inventory checkInventory(String orderId) {
        System.out.println("\n[NATIVE TOOL CALL] checkInventory(orderId=" + orderId + ")");
        var result = repository.inventory(orderId);
        System.out.println("[TOOL RESULT] " + result);
        System.out.println("[TOOL RETURN] Spring AI returns this result to the LLM.");
        return result;
    }
}
