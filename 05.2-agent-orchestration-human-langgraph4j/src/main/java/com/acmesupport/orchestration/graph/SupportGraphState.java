package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * The shared state that flows through every node in the graph. Each entry in
 * {@link #SCHEMA} declares a key and, via {@link Channels#base}, a default value to use
 * until some node sets it. Every field here uses simple "last write wins" semantics
 * (there's nothing accumulating across nodes in this workflow, unlike a chat-message
 * list) - {@code Channels.base(...)} is the right channel type for that.
 */
public class SupportGraphState extends AgentState {

    public static final String TICKET_ID = "ticketId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String TICKET_TEXT = "ticketText";
    public static final String CATEGORY = "category";
    public static final String PRIORITY = "priority";
    public static final String SENTIMENT = "sentiment";
    public static final String SUMMARY = "summary";
    public static final String RETRIEVED_CONTEXT = "retrievedContext";
    public static final String DRAFT_RESPONSE = "draftResponse";
    public static final String ESCALATION_REASON = "escalationReason";
    public static final String REVIEW_TYPE = "reviewType";

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(TICKET_ID, Channels.base(() -> "")),
            Map.entry(CUSTOMER_ID, Channels.base(() -> "")),
            Map.entry(TICKET_TEXT, Channels.base(() -> "")),
            Map.entry(CATEGORY, Channels.base(() -> "OTHER")),
            Map.entry(PRIORITY, Channels.base(() -> "MEDIUM")),
            Map.entry(SENTIMENT, Channels.base(() -> "NEUTRAL")),
            Map.entry(SUMMARY, Channels.base(() -> "")),
            Map.entry(RETRIEVED_CONTEXT, Channels.base(() -> "")),
            Map.entry(DRAFT_RESPONSE, Channels.base(() -> "")),
            Map.entry(ESCALATION_REASON, Channels.base(() -> "")),
            Map.entry(REVIEW_TYPE, Channels.base(() -> "")));

    public SupportGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public String ticketId() {
        return this.<String>value(TICKET_ID).orElse("");
    }

    public String customerId() {
        return this.<String>value(CUSTOMER_ID).orElse("");
    }

    public String ticketText() {
        return this.<String>value(TICKET_TEXT).orElse("");
    }

    public String category() {
        return this.<String>value(CATEGORY).orElse("OTHER");
    }

    public String priority() {
        return this.<String>value(PRIORITY).orElse("MEDIUM");
    }

    public String sentiment() {
        return this.<String>value(SENTIMENT).orElse("NEUTRAL");
    }

    public String summary() {
        return this.<String>value(SUMMARY).orElse("");
    }

    public String retrievedContext() {
        return this.<String>value(RETRIEVED_CONTEXT).orElse("");
    }

    public String draftResponse() {
        return this.<String>value(DRAFT_RESPONSE).orElse("");
    }

    public String escalationReason() {
        return this.<String>value(ESCALATION_REASON).orElse("");
    }

    public String reviewType() {
        return this.<String>value(REVIEW_TYPE).orElse("");
    }
}
