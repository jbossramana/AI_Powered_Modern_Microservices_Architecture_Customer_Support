package com.acmesupport.cleanarch.adapter.out.ai;

import com.acmesupport.cleanarch.application.port.out.ResponseGeneratorPort;
import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SpringAiResponseGeneratorAdapter implements ResponseGeneratorPort {

    private static final String SYSTEM_PROMPT = """
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Answer using ONLY the information in CONTEXT below. If CONTEXT doesn't
            cover the question, say a specialist will follow up - do not guess.
            """;

    private final ChatClient chatClient;

    public SpringAiResponseGeneratorAdapter(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public String generateReply(String ticketText, List<KnowledgeArticle> context) {
        String contextText = context.stream()
                .map(a -> "## " + a.title() + "\n" + a.content())
                .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
                .user(u -> u.text("Ticket:\n{ticket}\n\nCONTEXT:\n{context}")
                        .param("ticket", ticketText)
                        .param("context", contextText.isBlank() ? "No relevant articles found." : contextText))
                .call()
                .content();
    }
}
