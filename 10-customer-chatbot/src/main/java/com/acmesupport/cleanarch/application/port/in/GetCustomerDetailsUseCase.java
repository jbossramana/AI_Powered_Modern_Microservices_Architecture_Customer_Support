package com.acmesupport.cleanarch.application.port.in;

import com.acmesupport.cleanarch.domain.model.Customer;

/**
 * A new capability added to the existing series: given a customer ID, return their
 * account details. Notice this needed NO new outbound port - CustomerRepositoryPort
 * already had findById(), added back when TriageTicketService needed it. This use
 * case just composes an existing port for a new purpose, which is the payoff of
 * having defined ports around capabilities ("look up a customer") rather than around
 * endpoints ("the triage endpoint's customer lookup").
 */
public interface GetCustomerDetailsUseCase {

    /**
     * @throws com.acmesupport.cleanarch.domain.exception.CustomerNotFoundException if no customer has this ID
     */
    Customer getCustomerDetails(String customerId);
}
