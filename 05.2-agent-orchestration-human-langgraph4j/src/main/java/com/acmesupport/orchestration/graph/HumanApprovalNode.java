package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Marks the graph output as requiring an explicit human decision.
 * The graph ends here; the REST approval API records the asynchronous human decision.
 */
@Component
public class HumanApprovalNode implements NodeAction<SupportGraphState> {
    @Override
    public Map<String, Object> apply(SupportGraphState state) {
        String reviewType = state.escalationReason().isBlank() ? "RESPONSE_DRAFT" : "ESCALATION";
        return Map.of(SupportGraphState.REVIEW_TYPE, reviewType);
    }
}
