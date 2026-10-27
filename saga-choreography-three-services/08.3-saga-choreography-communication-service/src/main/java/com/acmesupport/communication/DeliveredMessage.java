package com.acmesupport.communication;

import java.time.Instant;

public record DeliveredMessage(String sagaId, String ticketId, String customerId, String outcome, String message, Instant deliveredAt) {
}
