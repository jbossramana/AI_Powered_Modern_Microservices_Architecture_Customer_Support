package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.Customer;
import com.acmesupport.cleanarch.domain.model.TicketClassification;

/**
 * An outbound port: something the application NEEDS from the outside world, defined in
 * the application's own vocabulary. It says nothing about how classification happens -
 * that's adapter/out/ai/SpringAiTicketClassifierAdapter's job (or, swapped in,
 * LangChain4jTicketClassifierAdapter's).
 */
public interface TicketClassifierPort {
    TicketClassification classify(String ticketText, Customer customer);
}
