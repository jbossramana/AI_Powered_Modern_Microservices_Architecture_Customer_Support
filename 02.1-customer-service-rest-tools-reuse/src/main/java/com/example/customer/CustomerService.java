package com.example.customer;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerService {

    private final Map<Long, Customer> customers = Map.of(
        101L, new Customer(101L, "Ravi", "Hyderabad", "GOLD"),
        102L, new Customer(102L, "Sita", "Bengaluru", "SILVER")
    );

    public Customer getCustomerById(Long id) {

        Customer customer = customers.get(id);

        if (customer == null) {
            throw new CustomerNotFoundException(id);
        }

        return customer;
    }
}