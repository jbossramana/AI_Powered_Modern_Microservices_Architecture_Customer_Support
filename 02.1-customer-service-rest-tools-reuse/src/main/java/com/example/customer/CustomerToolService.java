package com.example.customer;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerToolService {

	@Autowired
	CustomerService  customerService;

    @Tool(description = "Get customer details using customer ID")
    public Customer getCustomerById(Long id) {

        return customerService.getCustomerById(id);
        
    }
}