package com.acmesupport.ticket;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TicketStore {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public TicketStore() {
        tickets.put("TCK-2001", new Ticket("TCK-2001", "C-1001", "Jordan Lee",
                "Charged twice for the Pro plan this month", Ticket.Status.OPEN));
        tickets.put("TCK-2002", new Ticket("TCK-2002", "C-1002", "Amara Chen",
                "Renewal charge looks wrong for our Business workspace", Ticket.Status.OPEN));
    }

    public Optional<Ticket> find(String id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public Ticket hold(String id) {
        Ticket current = require(id);
        if (current.status() != Ticket.Status.OPEN) {
            throw new InvalidTicketStateException(id, current.status(), Ticket.Status.OPEN);
        }
        Ticket updated = current.withStatus(Ticket.Status.REFUND_IN_PROGRESS);
        tickets.put(id, updated);
        return updated;
    }

    public Ticket resolve(String id) {
        Ticket current = require(id);
        Ticket updated = current.withStatus(Ticket.Status.RESOLVED);
        tickets.put(id, updated);
        return updated;
    }

    public Ticket decline(String id) {
        Ticket current = require(id);
        Ticket updated = current.withStatus(Ticket.Status.REFUND_DECLINED);
        tickets.put(id, updated);
        return updated;
    }

    private Ticket require(String id) {
        return find(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
