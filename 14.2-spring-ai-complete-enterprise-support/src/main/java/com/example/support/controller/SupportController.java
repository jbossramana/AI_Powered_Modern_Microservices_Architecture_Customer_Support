package com.example.support.controller;

import com.example.support.model.ApprovalDecision;
import com.example.support.model.ApprovalRequest;
import com.example.support.model.InvestigationResult;
import com.example.support.model.SupportReply;
import com.example.support.model.TicketAnalysis;
import com.example.support.model.TicketRequest;
import com.example.support.service.AgentPatternsService;
import com.example.support.service.ApprovalService;
import com.example.support.service.AuditService;
import com.example.support.service.SupportAiService;
import com.example.support.service.SupportWorkflowService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportAiService supportAiService;
    private final AgentPatternsService agentPatternsService;
    private final SupportWorkflowService workflowService;
    private final ApprovalService approvalService;
    private final AuditService auditService;

    public SupportController(
            SupportAiService supportAiService,
            AgentPatternsService agentPatternsService,
            SupportWorkflowService workflowService,
            ApprovalService approvalService,
            AuditService auditService
    ) {
        this.supportAiService = supportAiService;
        this.agentPatternsService = agentPatternsService;
        this.workflowService = workflowService;
        this.approvalService = approvalService;
        this.auditService = auditService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('SUPPORT')")
    public SupportReply chat(
            @RequestBody @Valid TicketRequest request,
            Authentication authentication
    ) {
        String conversationId = ownedConversationId(
                authentication.getName(), request.conversationId(), request.ticketId());

        return supportAiService.answer(
                authentication.getName(),
                conversationId,
                request.customerMessage(),
                request.orderId()
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('SUPPORT')")
    public Flux<String> stream(
            @RequestParam String question,
            @RequestParam(defaultValue = "stream-demo") String conversationId,
            Authentication authentication
    ) {
        String ownedId = ownedConversationId(
                authentication.getName(), conversationId, "stream");
        return supportAiService.streamAnswer(authentication.getName(), ownedId, question);
    }

    @PostMapping("/tool-chat")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, String> toolChat(
            @RequestBody @Valid TicketRequest request,
            Authentication authentication
    ) {
        String conversationId = ownedConversationId(
                authentication.getName(), request.conversationId(), request.ticketId());

        return Map.of("answer", supportAiService.toolChat(
                authentication.getName(), conversationId, request.customerMessage()));
    }

    @PostMapping("/tickets/analyze")
    @PreAuthorize("hasRole('SUPPORT')")
    public TicketAnalysis analyzeTicket(
            @RequestBody @Valid TicketRequest request,
            Authentication authentication
    ) {
        return supportAiService.analyzeTicket(
                authentication.getName(),
                request.ticketId(),
                request.customerMessage()
        );
    }

    @PostMapping("/tickets/investigate")
    @PreAuthorize("hasRole('SUPPORT')")
    public InvestigationResult investigate(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String ticketId = required(request, "ticketId");
        String orderId = required(request, "orderId");
        String sku = request.getOrDefault("sku", "SKU-101");

        InvestigationResult result = workflowService.investigate(ticketId, orderId, sku);
        auditService.record(authentication.getName(), "DETERMINISTIC_INVESTIGATION",
                ticketId, "SUCCESS", "orderId=" + orderId + "; sku=" + sku);
        return result;
    }

    @PostMapping("/patterns/no-tool")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, String> noToolPattern(
            @RequestBody @Valid TicketRequest request,
            Authentication authentication
    ) {
        String conversationId = ownedConversationId(
                authentication.getName(), request.conversationId(), request.ticketId());
        return Map.of("answer", agentPatternsService.noToolAnswer(
                request.customerMessage(), conversationId));
    }

    @PostMapping("/patterns/plan-execute-synthesize")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, String> planExecuteSynthesize(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String question = required(request, "question");
        String ticketId = required(request, "ticketId");
        String orderId = required(request, "orderId");
        String sku = request.getOrDefault("sku", "SKU-101");

        auditService.record(authentication.getName(), "PLAN_EXECUTE_SYNTHESIZE",
                ticketId, "SUCCESS", "orderId=" + orderId);

        return Map.of("answer", agentPatternsService.planExecuteSynthesize(
                question, ticketId, orderId, sku));
    }

    @PostMapping("/patterns/reflect")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, String> reflectivePattern(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String question = required(request, "question");
        auditService.record(authentication.getName(), "REFLECTIVE_ANSWER",
                "support-question", "SUCCESS", "questionLength=" + question.length());
        return Map.of("answer", agentPatternsService.reflectiveAnswer(question));
    }

    @PostMapping("/patterns/multi-agent-route")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, String> multiAgentRoute(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String question = required(request, "question");
        String orderId = request.getOrDefault("orderId", "");

        auditService.record(authentication.getName(), "MULTI_AGENT_ROUTE",
                orderId, "SUCCESS", "questionLength=" + question.length());

        return Map.of("answer", agentPatternsService.multiAgentRoute(question, orderId));
    }

    @PostMapping("/approvals")
    @PreAuthorize("hasRole('SUPPORT')")
    public ResponseEntity<Map<String, Object>> requestApproval(
            @RequestBody @Valid ApprovalRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(approvalService.request(authentication.getName(), request));
    }

    @PostMapping("/approvals/{requestId}/decision")
    @PreAuthorize("hasRole('APPROVER')")
    public Map<String, Object> decide(
            @PathVariable String requestId,
            @RequestBody @Valid ApprovalDecision request,
            Authentication authentication
    ) {
        try {
            return approvalService.decide(
                    authentication.getName(), requestId, request.decision(), request.note());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @GetMapping("/approvals/{requestId}")
    @PreAuthorize("hasRole('SUPPORT')")
    public Map<String, Object> approval(@PathVariable String requestId) {
        try {
            return approvalService.get(requestId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('APPROVER')")
    public Object auditEvents() {
        return auditService.recentEvents();
    }

    private String ownedConversationId(String username, String suppliedId, String ticketId) {
        String id = suppliedId == null || suppliedId.isBlank()
                ? ticketId
                : suppliedId;

        if (id.contains(":") && !id.startsWith(username + ":")) {
            auditService.record(username, "CONVERSATION_ACCESS", id, "DENIED",
                    "Conversation owner mismatch");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Conversation is not owned by this user.");
        }

        return username + ":" + id.replace(":", "_");
    }

    private String required(Map<String, String> request, String key) {
        String value = request.get(key);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " is required.");
        }
        return value;
    }
}
