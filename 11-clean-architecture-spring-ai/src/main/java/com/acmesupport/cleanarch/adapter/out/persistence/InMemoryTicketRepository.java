package com.acmesupport.cleanarch.adapter.out.persistence;

import com.acmesupport.cleanarch.application.port.out.TicketRepositoryPort;
import com.acmesupport.cleanarch.domain.model.Ticket;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTicketRepository implements TicketRepositoryPort {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        tickets.put(ticket.id(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String id) {
        return Optional.ofNullable(tickets.get(id));
    }
}
