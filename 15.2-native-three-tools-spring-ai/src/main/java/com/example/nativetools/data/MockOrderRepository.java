package com.example.nativetools.data;

import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Map;

@Repository
public class MockOrderRepository {
    public record Payment(String orderId, String status, BigDecimal amount, String transactionId) {}
    public record Order(String orderId, String status, String shipmentStatus, String trackingId) {}
    public record Inventory(String orderId, String sku, String status, int availableQuantity) {}

    private final Map<String, Payment> payments = Map.of(
        "ORD-101", new Payment("ORD-101", "PAID", new BigDecimal("2500.00"), "TXN-101"),
        "ORD-102", new Payment("ORD-102", "FAILED", new BigDecimal("799.00"), "TXN-102"),
        "ORD-103", new Payment("ORD-103", "PAID", new BigDecimal("1200.00"), "TXN-103"));
    private final Map<String, Order> orders = Map.of(
        "ORD-101", new Order("ORD-101", "CONFIRMED", "DELAYED", "TRK-101"),
        "ORD-102", new Order("ORD-102", "PENDING_PAYMENT", "NOT_SHIPPED", "N/A"),
        "ORD-103", new Order("ORD-103", "CONFIRMED", "SHIPPED", "TRK-103"));
    private final Map<String, Inventory> inventory = Map.of(
        "ORD-101", new Inventory("ORD-101", "SKU-LAPTOP-01", "IN_STOCK", 8),
        "ORD-102", new Inventory("ORD-102", "SKU-HEADSET-02", "IN_STOCK", 24),
        "ORD-103", new Inventory("ORD-103", "SKU-MONITOR-03", "LOW_STOCK", 2));

    public Payment payment(String id) {
        return payments.getOrDefault(id, new Payment(id, "NOT_FOUND", BigDecimal.ZERO, "N/A"));
    }
    public Order order(String id) {
        return orders.getOrDefault(id, new Order(id, "NOT_FOUND", "NOT_FOUND", "N/A"));
    }
    public Inventory inventory(String id) {
        return inventory.getOrDefault(id, new Inventory(id, "UNKNOWN", "NOT_FOUND", 0));
    }
}
