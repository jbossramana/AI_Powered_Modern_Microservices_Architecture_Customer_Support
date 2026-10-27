package com.example.sequentialtools.data;

import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Map;

@Repository
public class MockOrderDataRepository {
	public record PaymentData(String orderId, String status, BigDecimal amount, String transactionId) {
	}

	public record OrderData(String orderId, String status, String shipmentStatus, String trackingId) {
	}

	public record InventoryData(String orderId, String sku, String availability, int availableQuantity) {
	}

	private final Map<String, PaymentData> payments = Map.of("ORD-101",
			new PaymentData("ORD-101", "PAID", new BigDecimal("2500.00"), "TXN-101"), "ORD-102",
			new PaymentData("ORD-102", "FAILED", new BigDecimal("799.00"), "TXN-102"), "ORD-103",
			new PaymentData("ORD-103", "PAID", new BigDecimal("1200.00"), "TXN-103"));
	private final Map<String, OrderData> orders = Map.of("ORD-101",
			new OrderData("ORD-101", "CONFIRMED", "DELAYED", "TRK-101"), "ORD-102",
			new OrderData("ORD-102", "PENDING_PAYMENT", "NOT_SHIPPED", "N/A"), "ORD-103",
			new OrderData("ORD-103", "CONFIRMED", "SHIPPED", "TRK-103"));
	private final Map<String, InventoryData> inventory = Map.of("ORD-101",
			new InventoryData("ORD-101", "SKU-LAPTOP-01", "IN_STOCK", 8), "ORD-102",
			new InventoryData("ORD-102", "SKU-HEADSET-02", "IN_STOCK", 24), "ORD-103",
			new InventoryData("ORD-103", "SKU-MONITOR-03", "LOW_STOCK", 2));

	public PaymentData getPayment(String id) {
		return payments.getOrDefault(id, new PaymentData(id, "NOT_FOUND", BigDecimal.ZERO, "N/A"));
	}

	public OrderData getOrder(String id) {
		return orders.getOrDefault(id, new OrderData(id, "NOT_FOUND", "NOT_FOUND", "N/A"));
	}

	public InventoryData getInventory(String id) {
		return inventory.getOrDefault(id, new InventoryData(id, "UNKNOWN", "NOT_FOUND", 0));
	}
}
