package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * First node in the graph. Reuses the exact structured-output pattern from Module 2 -
 * the only new idea here is that the result becomes a partial state update (a
 * {@code Map<String,Object>}) instead of a full HTTP response.
 */
@Component
public class TriageNode implements NodeAction<SupportGraphState> {

    private final ChatClient chatClient;

    public TriageNode(ChatClient orchestrationChatClient) {
        this.chatClient = orchestrationChatClient;
    }

    private record Triage(String category, String priority, String sentiment, String summary) {
    }

    @Override
    public Map<String, Object> apply(SupportGraphState state) {
        Triage result = chatClient.prompt()
                .system("""
                        Classify this support ticket.
                        category: one short label - ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, FEATURE_REQUEST, or OTHER.
                        priority: one of LOW, MEDIUM, HIGH, URGENT.
                        sentiment: one of POSITIVE, NEUTRAL, NEGATIVE, ANGRY.
                        summary: one sentence a support engineer can read in five seconds.
                        """)
                .user(state.ticketText())
                .call()
                .entity(Triage.class);

        return Map.of(
                SupportGraphState.CATEGORY, result.category(),
                SupportGraphState.PRIORITY, result.priority(),
                SupportGraphState.SENTIMENT, result.sentiment(),
                SupportGraphState.SUMMARY, result.summary());
    }
}
