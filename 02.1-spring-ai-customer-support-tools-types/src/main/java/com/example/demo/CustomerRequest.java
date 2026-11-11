package com.example.demo;

import java.util.List;

public record CustomerRequest(Long customerId, Double price, Integer discountPercentage, Boolean checkEligibility,
		List<String> requestedProducts) {
}
