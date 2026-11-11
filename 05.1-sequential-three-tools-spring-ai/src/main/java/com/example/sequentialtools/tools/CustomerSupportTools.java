package com.example.sequentialtools.tools;

import com.example.sequentialtools.data.MockOrderDataRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CustomerSupportTools {
	private final MockOrderDataRepository repository;

	public CustomerSupportTools(MockOrderDataRepository repository) {
		this.repository = repository;
	}

	@Tool(description = "Read payment status, amount and transaction ID for an order. Read-only.")
	public MockOrderDataRepository.PaymentData checkPayment(String orderId) {
		return repository.getPayment(orderId);
	}

	@Tool(description = "Read order status, shipment status and tracking ID for an order. Read-only.")
	public MockOrderDataRepository.OrderData checkOrder(String orderId) {
		return repository.getOrder(orderId);
	}

	@Tool(description = "Read inventory availability and available quantity for an order. Read-only.")
	public MockOrderDataRepository.InventoryData checkInventory(String orderId) {
		return repository.getInventory(orderId);
	}
}
