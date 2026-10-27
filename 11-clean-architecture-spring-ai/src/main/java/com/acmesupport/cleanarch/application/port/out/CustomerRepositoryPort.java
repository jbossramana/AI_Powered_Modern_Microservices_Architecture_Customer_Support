package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.Customer;

import java.util.Optional;

public interface CustomerRepositoryPort {
    Optional<Customer> findById(String customerId);
}
