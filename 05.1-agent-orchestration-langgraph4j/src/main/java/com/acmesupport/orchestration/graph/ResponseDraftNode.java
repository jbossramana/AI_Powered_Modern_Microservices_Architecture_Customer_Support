package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponseDraftNode implements NodeAction<SupportGraphState> {

    private final ChatClient chatClient;

    public ResponseDraftNode(ChatClient orchestrationChatClient) {
        this.chatClient = orchestrationChatClient;
    }

    @Override
    public Map<String, Object> apply(SupportGraphState state) {
        String draft = chatClient.prompt()
                .system("""
                        You are Aria, a customer support assistant for Acme Cloud Storage.
                        Draft a short, friendly reply to the customer using ONLY the
                        information in CONTEXT below. If CONTEXT doesn't answer their
                        question, say a specialist will follow up - do not guess.
                        """)
                .user(u -> u.text("""
                        Ticket:
                        {ticket}

                        CONTEXT:
                        {context}
                        """)
                        .param("ticket", state.ticketText())
                        .param("context", state.retrievedContext()))
                .call()
                .content();

        return Map.of(SupportGraphState.DRAFT_RESPONSE, draft);
    }
}
