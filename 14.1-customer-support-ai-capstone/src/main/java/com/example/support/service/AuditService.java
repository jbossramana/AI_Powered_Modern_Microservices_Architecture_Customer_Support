package com.example.support.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void record(String action, String subject, String outcome) {
        log.info("AUDIT timestamp={} action={} subject={} outcome={}",
                Instant.now(), safe(action), safe(subject), safe(outcome));
    }

    private String safe(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\\r\\n\\t]", " ");
    }
}
