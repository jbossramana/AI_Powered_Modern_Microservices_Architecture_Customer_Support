package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.application.port.in.TriageTicketUseCase;
import com.acmesupport.cleanarch.application.port.out.CustomerRepositoryPort;
import com.acmesupport.cleanarch.application.port.out.TicketClassifierPort;
import com.acmesupport.cleanarch.application.port.out.TicketRepositoryPort;
import com.acmesupport.cleanarch.domain.exception.CustomerNotFoundException;
import com.acmesupport.cleanarch.domain.model.Customer;
import com.acmesupport.cleanarch.domain.model.Ticket;
import com.acmesupport.cleanarch.domain.model.TicketClassification;
import com.acmesupport.cleanarch.domain.policy.EscalationPolicy;

import java.time.Instant;
import java.util.UUID;

/**
 * Notice what's NOT imported here: nothing from org.springframework, nothing from
 * org.springframework.ai. This class doesn't know Spring exists, doesn't know Ollama
 * exists, doesn't know whether classification happens via an LLM, a rules engine, or a
 * person typing into a spreadsheet - it only knows the TicketClassifierPort contract.
 * That's the Dependency Rule in practice, not just in a diagram, and it's what makes
 * TriageTicketServiceTest possible without Spring or a model running anywhere.
 */
public class TriageTicketService implements TriageTicketUseCase {

    private final TicketClassifierPort classifier;
    private final CustomerRepositoryPort customers;
    private final TicketRepositoryPort tickets;

    public TriageTicketService(
            TicketClassifierPort classifier, CustomerRepositoryPort customers, TicketRepositoryPort tickets) {
        this.classifier = classifier;
        this.customers = customers;
        this.tickets = tickets;
    }

    @Override
    public TriageResult triage(TriageCommand command) {
        Customer customer = customers.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

        TicketClassification classification = classifier.classify(command.ticketText(), customer);

        tickets.save(new Ticket(UUID.randomUUID().toString(), customer.id(), command.ticketText(), Instant.now()));

        boolean requiresEscalation = EscalationPolicy.requiresHumanEscalation(classification);
        return new TriageResult(classification, requiresEscalation);
    }
}
