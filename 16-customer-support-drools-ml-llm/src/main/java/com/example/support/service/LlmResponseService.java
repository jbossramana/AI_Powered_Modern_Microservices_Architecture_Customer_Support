package com.example.support.service;
import com.example.support.model.TicketFacts;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
@Service
public class LlmResponseService {
    private final ChatClient chatClient;
    public LlmResponseService(ChatClient.Builder builder){chatClient=builder.build();}
    public String draftReply(String ticketText, TicketFacts f) {
        String prompt = String.join("\n",
            "You are a customer support assistant. Draft a concise, professional response.",
            "Acknowledge the issue and explain the next step. Do not claim a fix is complete.",
            "Do not invent an estimated resolution time. Do not override application decisions.",
            "Customer ticket: " + ticketText,
            "Category: " + f.getCategory(),
            "Priority: " + f.getPriority(),
            "Assigned team: " + f.getAssignedTeam(),
            "Escalation required: " + f.isEscalate(),
            "Human approval required: " + f.isHumanApprovalRequired());
        String answer=chatClient.prompt().user(prompt).call().content();
        return answer==null || answer.isBlank() ? "Thank you for reporting this issue. Our support team will review it." : answer;
    }
}
