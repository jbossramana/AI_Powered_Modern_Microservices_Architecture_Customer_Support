package com.example.demo.service;

import com.example.demo.model.Customer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerService {
    private final Map<Long, Customer> customers = Map.of(
            101L, new Customer(101L, "Ravi", "Hyderabad", "GOLD"),
            102L, new Customer(102L, "Sita", "Bengaluru", "SILVER"),
            103L, new Customer(103L, "Kiran", "Chennai", "GOLD")
    );

    public Customer findById(Long id) {
        return customers.get(id);
    }

    @Tool(description = "Look up a customer by their numeric customer ID.")
    public Customer getCustomerById(
            @ToolParam(description = "Numeric customer ID, for example 101") Long id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            return null;
        }
        return customer;
    }
}
