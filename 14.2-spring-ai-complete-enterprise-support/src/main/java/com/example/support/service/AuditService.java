package com.example.support.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final List<Map<String, Object>> events = new CopyOnWriteArrayList<>();

    public void record(
            String actor,
            String action,
            String subject,
            String outcome,
            String details
    ) {
        events.add(Map.of(
                "time", Instant.now().toString(),
                "actor", actor,
                "action", action,
                "subject", subject == null ? "" : subject,
                "outcome", outcome,
                "details", details == null ? "" : details
        ));
    }

    public List<Map<String, Object>> recentEvents() {
        return List.copyOf(events);
    }
}
