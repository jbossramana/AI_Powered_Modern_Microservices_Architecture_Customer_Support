package com.acmesupport.cleanarch.adapter.out.persistence;

import com.acmesupport.cleanarch.application.port.out.CustomerRepositoryPort;
import com.acmesupport.cleanarch.domain.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * An in-memory stand-in for a real database. Swapping this for a JPA/JDBC-backed
 * implementation later means writing one new class implementing CustomerRepositoryPort
 * and pointing config/UseCaseConfig's @Bean wiring at it - nothing in application/ or
 * domain/ changes, for the same reason the AI adapters are swappable.
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepositoryPort {

    private final Map<String, Customer> customers = Map.of(
            "C-1001", new Customer("C-1001", "Jordan Lee", "jordan.lee@example.com", Customer.PlanTier.PRO),
            "C-1002", new Customer("C-1002", "Amara Chen", "amara.chen@example.com", Customer.PlanTier.BUSINESS));

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
