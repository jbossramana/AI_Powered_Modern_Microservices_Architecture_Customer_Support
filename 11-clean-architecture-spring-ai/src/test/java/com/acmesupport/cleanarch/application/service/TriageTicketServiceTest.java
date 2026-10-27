package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.application.port.in.TriageTicketUseCase.TriageCommand;
import com.acmesupport.cleanarch.application.port.out.CustomerRepositoryPort;
import com.acmesupport.cleanarch.application.port.out.TicketClassifierPort;
import com.acmesupport.cleanarch.application.port.out.TicketRepositoryPort;
import com.acmesupport.cleanarch.domain.model.Customer;
import com.acmesupport.cleanarch.domain.model.Ticket;
import com.acmesupport.cleanarch.domain.model.TicketClassification;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No {@code @SpringBootTest}, no Mockito, no Ollama running - just plain JUnit and
 * hand-written fakes of the two ports this use case depends on. This test runs in
 * milliseconds and is the entire payoff of everything else in this module: the
 * escalation decision is verified completely independently of infrastructure, and
 * would pass exactly the same way whether SpringAiTicketClassifierAdapter,
 * LangChain4jTicketClassifierAdapter, or a third framework entirely sits behind
 * TicketClassifierPort in production.
 */
class TriageTicketServiceTest {

    private static final Customer BUSINESS_CUSTOMER =
            new Customer("C-1002", "Amara Chen", "amara.chen@example.com", Customer.PlanTier.BUSINESS);

    @Test
    void urgentAndAngryClassificationRequiresEscalation() {
        var classification = new TicketClassification(
                TicketClassification.Category.ACCOUNT_ACCESS,
                TicketClassification.Priority.URGENT,
                TicketClassification.Sentiment.ANGRY,
                "Locked out repeatedly during business hours");

        var service = new TriageTicketService(
                new FakeClassifier(classification), new FakeCustomers(BUSINESS_CUSTOMER), new FakeTickets());

        var result = service.triage(new TriageCommand("C-1002", "Locked out again, unacceptable"));

        assertTrue(result.requiresHumanEscalation());
        assertEquals(TicketClassification.Priority.URGENT, result.classification().priority());
    }

    @Test
    void calmLowPriorityTicketDoesNotEscalate() {
        var classification = new TicketClassification(
                TicketClassification.Category.FEATURE_REQUEST,
                TicketClassification.Priority.LOW,
                TicketClassification.Sentiment.NEUTRAL,
                "Wants dark mode");

        var service = new TriageTicketService(
                new FakeClassifier(classification), new FakeCustomers(BUSINESS_CUSTOMER), new FakeTickets());

        var result = service.triage(new TriageCommand("C-1002", "Could you add dark mode?"));

        assertFalse(result.requiresHumanEscalation());
    }

    // --- Hand-written test doubles - no Mockito needed for a use case this small ---

    private record FakeClassifier(TicketClassification toReturn) implements TicketClassifierPort {
        @Override
        public TicketClassification classify(String ticketText, Customer customer) {
            return toReturn;
        }
    }

    private record FakeCustomers(Customer customer) implements CustomerRepositoryPort {
        @Override
        public Optional<Customer> findById(String customerId) {
            return customerId.equals(customer.id()) ? Optional.of(customer) : Optional.empty();
        }
    }

    private static class FakeTickets implements TicketRepositoryPort {
        private final Map<String, Ticket> store = new HashMap<>();

        @Override
        public Ticket save(Ticket ticket) {
            store.put(ticket.id(), ticket);
            return ticket;
        }

        @Override
        public Optional<Ticket> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }
    }
}
