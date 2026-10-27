package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Not every node needs to call a model. This one is pure Java: it formats why the
 * ticket is being handed to a human, based entirely on state earlier nodes already
 * produced. In a real system this would also write to a queue/ticketing system - see
 * Module 6 for a fuller human-in-the-loop approval flow this could feed into.
 */
@Component
public class EscalationNode implements NodeAction<SupportGraphState> {

    @Override
    public Map<String, Object> apply(SupportGraphState state) {
        String reason = "Routed to a human agent - priority=%s, sentiment=%s. %s"
                .formatted(state.priority(), state.sentiment(), state.summary());
        return Map.of(SupportGraphState.ESCALATION_REASON, reason);
    }
}
