package com.acmesupport.cleanarch.adapter.in.web;

import com.acmesupport.cleanarch.application.port.in.GetCustomerDetailsUseCase;
import com.acmesupport.cleanarch.domain.model.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

    private final GetCustomerDetailsUseCase getCustomerDetailsUseCase;

    public CustomerController(GetCustomerDetailsUseCase getCustomerDetailsUseCase) {
        this.getCustomerDetailsUseCase = getCustomerDetailsUseCase;
    }

    public record CustomerDetailsResponse(String customerId, String name, String email, String plan) {
        static CustomerDetailsResponse from(Customer customer) {
            return new CustomerDetailsResponse(customer.id(), customer.name(), customer.email(), customer.plan().name());
        }
    }

    /**
     * GET /api/customers/{customerId}
     *
     * A read, so GET with a path variable - unlike every other endpoint in this
     * module (all POST, since they trigger AI generation or a state change). Returns
     * 404 via CustomerNotFoundException -> ApiExceptionHandler, unchanged from what
     * that handler already did for TriageTicketService's use of the same exception.
     */
    @GetMapping("/api/customers/{customerId}")
    public CustomerDetailsResponse getCustomer(@PathVariable String customerId) {
        Customer customer = getCustomerDetailsUseCase.getCustomerDetails(customerId);
        return CustomerDetailsResponse.from(customer);
    }
}
