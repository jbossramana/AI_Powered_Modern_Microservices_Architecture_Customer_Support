package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.domain.exception.CustomerNotFoundException;
import com.acmesupport.cleanarch.domain.model.Customer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CustomerRepositoryPort has exactly one method, so it doubles as a functional
 * interface here - no fake class needed at all, just a lambda. Same "no Spring, no
 * Ollama, milliseconds" payoff as TriageTicketServiceTest, with even less ceremony.
 */
class GetCustomerDetailsServiceTest {

    private static final Customer JORDAN =
            new Customer("C-1001", "Jordan Lee", "jordan.lee@example.com", Customer.PlanTier.PRO);

    @Test
    void returnsCustomerWhenFound() {
        var service = new GetCustomerDetailsService(id -> id.equals("C-1001") ? Optional.of(JORDAN) : Optional.empty());

        Customer result = service.getCustomerDetails("C-1001");

        assertEquals("Jordan Lee", result.name());
        assertEquals(Customer.PlanTier.PRO, result.plan());
    }

    @Test
    void throwsWhenNotFound() {
        var service = new GetCustomerDetailsService(id -> Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.getCustomerDetails("C-9999"));
    }
}
