package com.acmesupport.cleanarch.domain.model;

import java.time.Instant;

public record Ticket(String id, String customerId, String text, Instant createdAt) {
}
