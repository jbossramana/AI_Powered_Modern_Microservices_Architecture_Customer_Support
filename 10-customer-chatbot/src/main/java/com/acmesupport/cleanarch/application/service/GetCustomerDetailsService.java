package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.application.port.in.GetCustomerDetailsUseCase;
import com.acmesupport.cleanarch.application.port.out.CustomerRepositoryPort;
import com.acmesupport.cleanarch.domain.exception.CustomerNotFoundException;
import com.acmesupport.cleanarch.domain.model.Customer;

/**
 * As plain as a use case gets: one dependency, one lookup, one domain exception if it
 * fails. Still worth its own class rather than letting adapter/in/web/CustomerController
 * call CustomerRepositoryPort directly - controllers depend on use cases (port/in),
 * never on outbound ports (port/out) directly, even when the use case would otherwise
 * be "just" a pass-through. That boundary is what keeps CustomerRepositoryPort free to
 * change independently of the web contract.
 */
public class GetCustomerDetailsService implements GetCustomerDetailsUseCase {

    private final CustomerRepositoryPort customers;

    public GetCustomerDetailsService(CustomerRepositoryPort customers) {
        this.customers = customers;
    }

    @Override
    public Customer getCustomerDetails(String customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
