package com.acmesupport.communication;

import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/internal/communications")
public class CommunicationController {

    private final ChatClient chatClient;
    private final List<DeliveredMessage> deliveryLog = new CopyOnWriteArrayList<>();

    public CommunicationController(ChatClient communicationChatClient) {
        this.chatClient = communicationChatClient;
    }

    public record RefundOutcomeRequest(
            @NotBlank String ticketId,
            @NotBlank String customerId,
            @NotBlank String customerName,
            @NotBlank String outcome, // "APPROVED" or "DECLINED"
            long amountCents,
            String reason // populated when outcome is DECLINED
    ) {
    }

    public record DraftResponse(String message) {
    }

    public record DeliveredMessage(String ticketId, String customerId, String outcome, String message, Instant deliveredAt) {
    }

    /**
     * Called once per saga run, after the orchestrator already knows the final
     * outcome - this endpoint's job is purely to phrase it for the customer and
     * record that it was "sent" (logged here rather than actually emailed, same
     * simplification as every other notification in this series).
     */
    @PostMapping("/refund-outcome")
    public DraftResponse handleOutcome(@RequestBody RefundOutcomeRequest request) {
        String amount = "$%.2f".formatted(request.amountCents() / 100.0);

        String message = chatClient.prompt()
                .user(u -> u.text("""
                        Customer: {name}
                        Ticket: {ticketId}
                        Outcome: {outcome}
                        Amount: {amount}
                        Reason (only relevant if declined): {reason}
                        """)
                        .param("name", request.customerName())
                        .param("ticketId", request.ticketId())
                        .param("outcome", request.outcome())
                        .param("amount", amount)
                        .param("reason", request.reason() == null ? "n/a" : request.reason()))
                .call()
                .content();

        deliveryLog.add(new DeliveredMessage(request.ticketId(), request.customerId(), request.outcome(), message, Instant.now()));

        return new DraftResponse(message);
    }

    /** Inspection endpoint - lets you verify a message was actually "delivered" during testing. */
    @GetMapping
    public List<DeliveredMessage> listDelivered() {
        return deliveryLog;
    }
}
