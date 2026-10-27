package com.example.support.controller;

import com.example.support.model.ApprovalRequest;
import com.example.support.model.ChatRequest;
import com.example.support.model.TicketRequest;
import com.example.support.service.AuditService;
import com.example.support.service.ConversationService;
import com.example.support.service.TicketAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class SupportController {
    private final TicketAiService ai;
    private final ConversationService conversations;
    private final AuditService audit;
    private final Map<String, String> pendingDrafts = new ConcurrentHashMap<>();

    public SupportController(TicketAiService ai, ConversationService conversations, AuditService audit) {
        this.ai = ai;
        this.conversations = conversations;
        this.audit = audit;
    }

    @PostMapping("/tickets/classify")
    public String classify(@Valid @RequestBody TicketRequest request) {
        return ai.classify(request.ticketId(), request.customerMessage());
    }

    @PostMapping("/documents/summarize")
    public String summarize(@RequestBody Map<String, String> body) {
        return ai.summarize(required(body, "text"));
    }

    @PostMapping("/knowledge/ask")
    public String askKnowledge(@RequestBody Map<String, String> body) {
        return ai.answerWithRag(required(body, "question"));
    }

    @PostMapping("/tool-calling")
    public String toolCalling(@RequestBody Map<String, String> body) {
        return ai.modelToolCalling(required(body, "question"));
    }

    @PostMapping("/investigations")
    public String investigate(@RequestBody Map<String, String> body) {
        return ai.investigate(required(body, "orderId"),
                required(body, "productName"), required(body, "customerMessage"));
    }

    @PostMapping("/responses/draft")
    public ResponseEntity<Map<String, String>> draft(@RequestBody Map<String, String> body) {
        String ticketId = required(body, "ticketId");
        String draft = ai.suggestResponse(ticketId, required(body, "customerMessage"));
        pendingDrafts.put(ticketId, draft);
        return ResponseEntity.ok(Map.of(
                "ticketId", ticketId,
                "status", "PENDING_HUMAN_APPROVAL",
                "draft", draft));
    }

    @PostMapping("/responses/approval")
    public ResponseEntity<Map<String, String>> approve(@Valid @RequestBody ApprovalRequest request) {
        String existing = pendingDrafts.get(request.ticketId());
        if (existing == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "NO_PENDING_DRAFT",
                    "message", "Create a draft before submitting an approval decision."));
        }
        if (!request.approved()) {
            pendingDrafts.remove(request.ticketId());
            audit.record("response_approval", request.ticketId(),
                    "rejected_by_" + request.approvedBy());
            return ResponseEntity.ok(Map.of("status", "REJECTED", "ticketId", request.ticketId()));
        }

        // Demo only: this marks approval. It does not send an email or call an external system.
        pendingDrafts.remove(request.ticketId());
        audit.record("response_approval", request.ticketId(),
                "approved_by_" + request.approvedBy());
        return ResponseEntity.ok(Map.of(
                "status", "APPROVED_DEMO_ONLY",
                "ticketId", request.ticketId(),
                "approvedResponse", request.responseText()));
    }

    @PostMapping("/chat")
    public String chat(@Valid @RequestBody ChatRequest request) {
        return conversations.chat(request.conversationId(), request.message());
    }

    @GetMapping("/demo-data")
    public Map<String, Object> demoData() {
        return Map.of(
                "orders", Map.of(
                        "ORD-10025", "Laptop; shipment delayed; payment successful",
                        "ORD-10026", "Headphones; shipped; payment successful"),
                "note", "Demo data only. All tools are read-only.");
    }

    private String required(Map<String, String> body, String key) {
        String value = body == null ? null : body.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }
}
