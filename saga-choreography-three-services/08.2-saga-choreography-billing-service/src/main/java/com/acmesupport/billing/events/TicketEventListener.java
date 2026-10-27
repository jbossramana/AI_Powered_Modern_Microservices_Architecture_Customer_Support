package com.acmesupport.billing.events;

import com.acmesupport.billing.Refund;
import com.acmesupport.billing.RefundPolicyViolationException;
import com.acmesupport.billing.RefundStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Billing-service's half of the choreography. Notice this class needs to know about
 * TicketResolutionFailed - an event describing something that happened in
 * ticket-service, conceptually downstream of billing's own step. That cross-service
 * awareness is the real-world cost choreography pays for not having a central
 * orchestrator: the knowledge of "who compensates when" ends up distributed across
 * whichever services need to react to it, rather than living in one place.
 */
@Component
public class TicketEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventListener.class);

    private final RefundStore refunds;
    private final EventPublisher publisher;
    private final ObjectMapper objectMapper;

    public TicketEventListener(RefundStore refunds, EventPublisher publisher, ObjectMapper objectMapper) {
        this.refunds = refunds;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${topics.ticket-events}", groupId = "billing-service")
    public void onTicketEvent(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("type").asText("");
            switch (type) {
                case "TicketRefundHeld" -> handleTicketRefundHeld(objectMapper.readValue(json, TicketEvents.TicketRefundHeld.class));
                case "TicketResolutionFailed" -> handleTicketResolutionFailed(objectMapper.readValue(json, TicketEvents.TicketResolutionFailed.class));
                // TicketRefundResolved, TicketRefundDeclined, RefundOutcomeReady also
                // flow through this topic but billing-service has no reason to react
                // to them - it doesn't subscribe blindly to everything, only to what
                // it needs, which is exactly what keeps a choreography saga from
                // turning into every service listening to everything.
                default -> log.debug("Ignoring ticket event not relevant to billing: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process ticket event: {}", json, e);
        }
    }

    private void handleTicketRefundHeld(TicketEvents.TicketRefundHeld event) {
        try {
            Refund refund = refunds.process(event.sagaId(), event.ticketId(), event.customerId(), event.amountCents());
            publisher.publish(event.ticketId(), new BillingEvents.RefundProcessed(
                    event.sagaId(), event.ticketId(), event.customerId(), refund.id(), event.amountCents()));
        } catch (RefundPolicyViolationException policyViolation) {
            log.info("[{}] refund declined by policy: {}", event.sagaId(), policyViolation.getMessage());
            publisher.publish(event.ticketId(), new BillingEvents.RefundDeclined(
                    event.sagaId(), event.ticketId(), event.customerId(), event.amountCents(), policyViolation.getMessage()));
        }
    }

    /** The compensation: billing reversing money it already moved, triggered entirely by an event, not a direct call. */
    private void handleTicketResolutionFailed(TicketEvents.TicketResolutionFailed event) {
        log.warn("[{}] reversing refund {} - ticket resolution failed downstream", event.sagaId(), event.refundId());
        Refund reversed = refunds.reverse(event.refundId());
        publisher.publish(event.ticketId(), new BillingEvents.RefundReversed(
                event.sagaId(), event.ticketId(), event.refundId(), reversed.amountCents()));
    }
}
