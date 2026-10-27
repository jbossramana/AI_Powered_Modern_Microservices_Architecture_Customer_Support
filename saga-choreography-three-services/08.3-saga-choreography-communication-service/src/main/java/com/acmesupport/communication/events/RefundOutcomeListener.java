package com.acmesupport.communication.events;

import com.acmesupport.communication.DeliveredMessage;
import com.acmesupport.communication.DeliveryLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefundOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(RefundOutcomeListener.class);

    private final ChatClient chatClient;
    private final DeliveryLog deliveryLog;
    private final ObjectMapper objectMapper;

    public RefundOutcomeListener(ChatClient communicationChatClient, DeliveryLog deliveryLog, ObjectMapper objectMapper) {
        this.chatClient = communicationChatClient;
        this.deliveryLog = deliveryLog;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${topics.refund-outcomes}", groupId = "communication-service")
    public void onRefundOutcomeReady(String json) {
        try {
            RefundOutcomeReady event = objectMapper.readValue(json, RefundOutcomeReady.class);
            String amount = "$%.2f".formatted(event.amountCents() / 100.0);

            String message = chatClient.prompt()
                    .user(u -> u.text("""
                            Customer: {name}
                            Ticket: {ticketId}
                            Outcome: {outcome}
                            Amount: {amount}
                            Reason (only relevant if declined): {reason}
                            """)
                            .param("name", event.customerName())
                            .param("ticketId", event.ticketId())
                            .param("outcome", event.outcome())
                            .param("amount", amount)
                            .param("reason", event.reason() == null ? "n/a" : event.reason()))
                    .call()
                    .content();

            deliveryLog.record(new DeliveredMessage(
                    event.sagaId(), event.ticketId(), event.customerId(), event.outcome(), message, Instant.now()));

            log.info("[{}] drafted and delivered {} message for ticket {}", event.sagaId(), event.outcome(), event.ticketId());
        } catch (Exception e) {
            // This is the saga's last step - if it fails, the customer never gets
            // told anything, which is worse than any earlier failure in the chain.
            // A production system would retry this specific step or alert on it
            // rather than only logging, since there's no further compensation
            // possible once billing and ticket-service have already settled.
            log.error("Failed to draft/deliver outcome message: {}", json, e);
        }
    }
}
