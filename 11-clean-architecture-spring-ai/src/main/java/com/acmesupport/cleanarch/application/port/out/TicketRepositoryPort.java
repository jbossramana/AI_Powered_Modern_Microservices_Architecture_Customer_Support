package com.acmesupport.cleanarch.application.port.out;

import com.acmesupport.cleanarch.domain.model.Ticket;

import java.util.Optional;

public interface TicketRepositoryPort {
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String id);
}
