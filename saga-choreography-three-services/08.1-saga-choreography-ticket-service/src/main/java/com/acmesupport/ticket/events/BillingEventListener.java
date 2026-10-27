package com.acmesupport.ticket.events;

import com.acmesupport.ticket.SagaTracker;
import com.acmesupport.ticket.Ticket;
import com.acmesupport.ticket.TicketStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * This class IS the choreography, from ticket-service's side: nothing here was told
 * by a central coordinator to run - each method fires because a message arrived on a
 * topic this service chose to subscribe to, and each method decides for itself what
 * to do next (including, in handleRefundProcessed, whether to trigger a compensation
 * by publishing TicketResolutionFailed). Compare RefundSagaOrchestrator in the
 * orchestration version: there, this entire sequence was one method, readable top to
 * bottom. Here, it's inherently split across this class and TicketEventListener in
 * billing-service, each only aware of the events it subscribes to.
 */
@Component
public class BillingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BillingEventListener.class);

    private final TicketStore tickets;
    private final SagaTracker sagaTracker;
    private final EventPublisher publisher;
    private final ObjectMapper objectMapper;

    public BillingEventListener(TicketStore tickets, SagaTracker sagaTracker, EventPublisher publisher, ObjectMapper objectMapper) {
        this.tickets = tickets;
        this.sagaTracker = sagaTracker;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${topics.billing-events}", groupId = "ticket-service")
    public void onBillingEvent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("type").asText("");
            switch (type) {
                case "RefundProcessed" -> handleRefundProcessed(objectMapper.readValue(json, BillingEvents.RefundProcessed.class));
                case "RefundDeclined" -> handleRefundDeclined(objectMapper.readValue(json, BillingEvents.RefundDeclined.class));
                case "RefundReversed" -> handleRefundReversed(objectMapper.readValue(json, BillingEvents.RefundReversed.class));
                default -> log.warn("Ignoring unrecognized billing event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process billing event: {}", json, e);
        }
    }

    private void handleRefundProcessed(BillingEvents.RefundProcessed event) {
        boolean simulateFailure = sagaTracker.find(event.sagaId())
                .map(SagaTracker.SagaState::simulateFailureAfterBilling)
                .orElse(false);

        if (simulateFailure) {
            log.warn("[{}] simulateFailureAfterBilling is set - failing confirmation on purpose", event.sagaId());
            publisher.publish(event.ticketId(), new TicketEvents.TicketResolutionFailed(
                    event.sagaId(), event.ticketId(), event.refundId(),
                    "Simulated failure after billing already succeeded (test-only hook)."));
            return;
        }

        Ticket ticket = tickets.resolve(event.ticketId());
        sagaTracker.updateStatus(event.sagaId(), "APPROVED");
        publisher.publish(event.ticketId(), new TicketEvents.TicketRefundResolved(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(), event.amountCents()));
        publisher.publish(event.ticketId(), new TicketEvents.RefundOutcomeReady(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(),
                "APPROVED", event.amountCents(), null));
    }

    private void handleRefundDeclined(BillingEvents.RefundDeclined event) {
        Ticket ticket = tickets.decline(event.ticketId());
        sagaTracker.updateStatus(event.sagaId(), "DECLINED");
        publisher.publish(event.ticketId(), new TicketEvents.TicketRefundDeclined(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(), event.amountCents(), event.reason()));
        publisher.publish(event.ticketId(), new TicketEvents.RefundOutcomeReady(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(),
                "DECLINED", event.amountCents(), event.reason()));
    }

    private void handleRefundReversed(BillingEvents.RefundReversed event) {
        // The compensation loop closing: billing already reversed the money because
        // OUR OWN TicketResolutionFailed event told it to (see TicketEventListener in
        // billing-service). Now we release our own hold to match.
        Ticket ticket = tickets.decline(event.ticketId());
        String reason = "The refund could not be confirmed after processing, and was reversed.";
        sagaTracker.updateStatus(event.sagaId(), "DECLINED");
        publisher.publish(event.ticketId(), new TicketEvents.TicketRefundDeclined(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(), event.amountCents(), reason));
        publisher.publish(event.ticketId(), new TicketEvents.RefundOutcomeReady(
                event.sagaId(), event.ticketId(), ticket.customerId(), ticket.customerName(),
                "DECLINED", event.amountCents(), reason));
    }
}
