package com.acmesupport.ticket;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String ticketId) {
        super("No ticket found with id " + ticketId);
    }
}
