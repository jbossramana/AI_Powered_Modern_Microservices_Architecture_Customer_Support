package com.example.support.service;

import com.example.support.model.TicketFacts;
import com.example.support.model.TicketRequest;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Service
public class TicketService {
    private final MlClassifierClient ml;
    private final LlmResponseService llm;
    private final KieBase supportKieBase;

    public TicketService(MlClassifierClient ml, LlmResponseService llm, KieBase supportKieBase) {
        this.ml = ml;
        this.llm = llm;
        this.supportKieBase = supportKieBase;
    }

    public TicketResult process(TicketRequest req) {
        var prediction = ml.predict(req.ticketText());
        var facts = new TicketFacts(
                prediction.category(), prediction.priority(), prediction.confidence(),
                req.enterpriseCustomer());

        KieSession session = supportKieBase.newKieSession();
        try {
            session.insert(facts);
            session.fireAllRules();
        } finally {
            session.dispose();
        }

        String reply = llm.draftReply(req.ticketText(), facts);
        return new TicketResult(
                req.ticketId(), req.customerId(), facts.getCategory(), facts.getPriority(),
                prediction.confidence(), facts.getAssignedTeam(), facts.isEscalate(),
                facts.isHumanApprovalRequired(), facts.getReason(), reply);
    }

    public record TicketResult(
            String ticketId, String customerId, String category, String priority,
            double confidence, String assignedTeam, boolean escalated,
            boolean humanApprovalRequired, String reason, String suggestedReply) {
    }
}
