package com.acmesupport.hitl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DraftAndApprovalService {

    private static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage, drafting
            a reply for a human support engineer to review before it is sent - you are
            NOT sending this directly to the customer. Be helpful, concise, and honest
            about anything you're unsure of; the engineer will fix mistakes, but a
            confident wrong answer is worse than a flagged uncertain one.
            """;

    private final ChatClient chatClient;
    private final Map<String, PendingApproval> approvals = new ConcurrentHashMap<>();

    public DraftAndApprovalService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /** Step 1: the AI drafts a reply. Nothing is sent yet - it just enters the queue as PENDING. */
    public PendingApproval draftForReview(String ticketId, String customerId, String ticketText) {
        String draft = chatClient.prompt()
                .user(ticketText)
                .call()
                .content();

        String id = UUID.randomUUID().toString();
        PendingApproval approval = PendingApproval.newDraft(id, ticketId, customerId, draft);
        approvals.put(id, approval);
        return approval;
    }

    public List<PendingApproval> listByStatus(PendingApproval.Status status) {
        return approvals.values().stream()
                .filter(a -> a.status() == status)
                .sorted(Comparator.comparing(PendingApproval::createdAt))
                .toList();
    }

    public PendingApproval get(String id) {
        PendingApproval approval = approvals.get(id);
        if (approval == null) {
            throw new NoSuchElementException("No approval item with id " + id);
        }
        return approval;
    }

    /** Step 2a: a support engineer approves the draft as-is, or with edits. */
    public PendingApproval approve(String id, String reviewer, String editedTextOrNull) {
        PendingApproval current = get(id);
        String finalText = (editedTextOrNull == null || editedTextOrNull.isBlank())
                ? current.draftResponse()
                : editedTextOrNull;
        PendingApproval updated = current.approve(reviewer, finalText);
        approvals.put(id, updated);
        return updated;
    }

    /** Step 2b: a support engineer rejects the draft outright - it will never be sent. */
    public PendingApproval reject(String id, String reviewer, String reason) {
        PendingApproval updated = get(id).reject(reviewer, reason);
        approvals.put(id, updated);
        return updated;
    }

    /**
     * Step 3: actually "send" the response. This is the enforcement point - the whole
     * reason this module exists. It's not optional or bypassable through some other
     * endpoint: nothing in this service can transition PENDING -> SENT directly.
     */
    public PendingApproval send(String id) {
        PendingApproval current = get(id);
        if (current.status() != PendingApproval.Status.APPROVED) {
            throw new IllegalStateException(
                    "Cannot send item %s: status is %s, not APPROVED".formatted(id, current.status()));
        }
        PendingApproval sent = current.markSent();
        approvals.put(id, sent);
        return sent;
    }
}
