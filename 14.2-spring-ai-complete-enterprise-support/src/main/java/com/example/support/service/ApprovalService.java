package com.example.support.service;

import com.example.support.model.ApprovalRequest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private final Map<String, Map<String, Object>> requests = new ConcurrentHashMap<>();
    private final AuditService auditService;

    public ApprovalService(AuditService auditService) {
        this.auditService = auditService;
    }

    public Map<String, Object> request(String actor, ApprovalRequest request) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> item = Map.of(
                "requestId", id,
                "ticketId", request.ticketId(),
                "action", request.action(),
                "reason", request.reason(),
                "status", "PENDING",
                "createdAt", Instant.now().toString(),
                "requestedBy", actor
        );
        requests.put(id, item);
        auditService.record(actor, "APPROVAL_REQUEST", id, "PENDING",
                "ticketId=" + request.ticketId() + "; action=" + request.action());
        return item;
    }

    public Map<String, Object> decide(
            String approver,
            String requestId,
            String decision,
            String note
    ) {
        Map<String, Object> existing = requests.get(requestId);
        if (existing == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }
        if (!"PENDING".equals(existing.get("status"))) {
            throw new IllegalStateException("Approval request has already been decided.");
        }

        String normalized = decision.toUpperCase();
        if (!normalized.equals("APPROVE") && !normalized.equals("REJECT")) {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }

        Map<String, Object> updated = new java.util.HashMap<>(existing);
        updated.put("status", normalized);
        updated.put("decidedAt", Instant.now().toString());
        updated.put("decidedBy", approver);
        updated.put("note", note == null ? "" : note);
        updated.put("executionStatus", "NOT_EXECUTED_DEMO_ONLY");
        requests.put(requestId, Map.copyOf(updated));

        auditService.record(approver, "APPROVAL_DECISION", requestId, normalized,
                "Demo records decision only; no external action executed.");
        return requests.get(requestId);
    }

    public Map<String, Object> get(String requestId) {
        Map<String, Object> request = requests.get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Approval request not found: " + requestId);
        }
        return request;
    }
}
