package com.acmesupport.orchestrator.saga.steps;

import com.acmesupport.orchestrator.saga.RefundSagaContext;
import com.acmesupport.orchestrator.saga.SagaStep;
import com.acmesupport.orchestrator.saga.SagaStepException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HoldTicketStep implements SagaStep<RefundSagaContext> {

    private record TicketView(String id, String customerId, String customerName, String subject, String status) {
    }

    private final RestClient ticketServiceClient;
    private final ObjectMapper objectMapper;

    public HoldTicketStep(@Qualifier("ticketServiceClient") RestClient ticketServiceClient, ObjectMapper objectMapper) {
        this.ticketServiceClient = ticketServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "hold-ticket";
    }

    @Override
    public void execute(RefundSagaContext ctx) {
        try {
            TicketView ticket = ticketServiceClient.post()
                    .uri("/internal/tickets/{id}/refund-hold", ctx.ticketId())
                    .retrieve()
                    .body(TicketView.class);
            ctx.setCustomerId(ticket.customerId());
            ctx.setCustomerName(ticket.customerName());
        } catch (RestClientResponseException e) {
            throw new SagaStepException(RemoteErrors.extract(e, objectMapper, "The ticket couldn't be put on hold."));
        }
    }

    @Override
    public void compensate(RefundSagaContext ctx) {
        ticketServiceClient.post()
                .uri("/internal/tickets/{id}/refund-hold/release", ctx.ticketId())
                .retrieve()
                .toBodilessEntity();
    }
}
