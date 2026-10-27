package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * The graph's shape:
 *
 * <pre>
 *              START
 *                |
 *             triage
 *            /        \
 *   (urgent+negative)  (everything else)
 *          |                  |
 *      escalate      knowledge_retrieval
 *          |                  |
 *          +             draft_response
 *           \                /
 *              human_approval
 *                    |
 *                   END
 * </pre>
 *
 * Every node is a Spring bean, injected here and wrapped with {@code node_async(...)}.
 * The routing decision itself ({@link #routeAfterTriage}) is a plain static function -
 * easy to unit test with a hand-built {@code SupportGraphState}, independent of any
 * model call.
 */
@Configuration
public class SupportGraphBuilder {

    @Bean
    CompiledGraph<SupportGraphState> supportGraph(
            TriageNode triageNode,
            KnowledgeRetrievalNode knowledgeRetrievalNode,
            ResponseDraftNode responseDraftNode,
            EscalationNode escalationNode,
            HumanApprovalNode humanApprovalNode) throws Exception {

        StateGraph<SupportGraphState> workflow = new StateGraph<>(SupportGraphState.SCHEMA, SupportGraphState::new)
                .addNode("triage", node_async(triageNode))
                .addNode("knowledge_retrieval", node_async(knowledgeRetrievalNode))
                .addNode("draft_response", node_async(responseDraftNode))
                .addNode("escalate", node_async(escalationNode))
                .addNode("human_approval", node_async(humanApprovalNode))
                .addEdge(START, "triage")
                .addConditionalEdges("triage", edge_async(SupportGraphBuilder::routeAfterTriage), Map.of(
                        "escalate", "escalate",
                        "respond", "knowledge_retrieval"))
                .addEdge("knowledge_retrieval", "draft_response")
                .addEdge("draft_response", "human_approval")
                .addEdge("escalate", "human_approval")
                .addEdge("human_approval", END);

        return workflow.compile();
    }

    /**
     * The routing policy: high-or-urgent priority combined with negative-or-angry
     * sentiment takes the escalation branch. Both branches then pass through the explicit
     * human_approval node before the graph ends, so every processed ticket is recorded
     * for a human decision. The routing policy is deterministic and testable.
     */
    static String routeAfterTriage(SupportGraphState state) {
        boolean highUrgency = "URGENT".equals(state.priority()) || "HIGH".equals(state.priority());
        boolean negativeTone = "ANGRY".equals(state.sentiment()) || "NEGATIVE".equals(state.sentiment());
        return (highUrgency && negativeTone) ? "escalate" : "respond";
    }
}
