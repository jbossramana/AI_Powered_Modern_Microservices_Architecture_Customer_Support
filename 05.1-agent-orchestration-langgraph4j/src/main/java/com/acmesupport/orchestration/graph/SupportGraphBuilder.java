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
 *         END          draft_response
 *                              |
 *                             END
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
            EscalationNode escalationNode) throws Exception {

        StateGraph<SupportGraphState> workflow = new StateGraph<>(SupportGraphState.SCHEMA, SupportGraphState::new)
                .addNode("triage", node_async(triageNode))
                .addNode("knowledge_retrieval", node_async(knowledgeRetrievalNode))
                .addNode("draft_response", node_async(responseDraftNode))
                .addNode("escalate", node_async(escalationNode))
                .addEdge(START, "triage")
                .addConditionalEdges("triage", edge_async(SupportGraphBuilder::routeAfterTriage), Map.of(
                        "escalate", "escalate",
                        "respond", "knowledge_retrieval"))
                .addEdge("knowledge_retrieval", "draft_response")
                .addEdge("draft_response", END)
                .addEdge("escalate", END);

        return workflow.compile();
    }

    /**
     * The routing policy: high-or-urgent priority combined with negative-or-angry
     * sentiment skips straight to a human. Everything else goes through retrieval and
     * gets an AI-drafted reply. Encoding this as code (not just prompt instructions)
     * means the branch is guaranteed, testable, and auditable - the same philosophy as
     * the escalation policy guardrail in Module 2.
     */
    static String routeAfterTriage(SupportGraphState state) {
        boolean highUrgency = "URGENT".equals(state.priority()) || "HIGH".equals(state.priority());
        boolean negativeTone = "ANGRY".equals(state.sentiment()) || "NEGATIVE".equals(state.sentiment());
        return (highUrgency && negativeTone) ? "escalate" : "respond";
    }
}
