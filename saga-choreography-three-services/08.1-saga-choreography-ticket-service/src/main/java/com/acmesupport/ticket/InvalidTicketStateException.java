package com.acmesupport.ticket;

public class InvalidTicketStateException extends RuntimeException {
    public InvalidTicketStateException(String ticketId, Ticket.Status actual, Ticket.Status required) {
        super("Ticket %s is %s, which cannot transition from the required %s state"
                .formatted(ticketId, actual, required));
    }
}
