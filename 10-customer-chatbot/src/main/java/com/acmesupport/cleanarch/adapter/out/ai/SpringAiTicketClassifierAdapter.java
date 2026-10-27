package com.acmesupport.cleanarch.adapter.out.ai;

import com.acmesupport.cleanarch.application.port.out.TicketClassifierPort;
import com.acmesupport.cleanarch.domain.model.Customer;
import com.acmesupport.cleanarch.domain.model.TicketClassification;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * The ONLY class in this codebase that knows Spring AI's ChatClient exists for
 * classification purposes. It translates between a private, prompting-shaped DTO and
 * the framework-agnostic domain type TicketClassification the rest of the app works
 * with. @Component is fine here (unlike in application/service) - adapters are
 * expected to know about the infrastructure they adapt; that's their entire purpose.
 */
@Component
public class SpringAiTicketClassifierAdapter implements TicketClassifierPort {

    private static final String SYSTEM_PROMPT = """
            Classify this customer support ticket.
            category: one of ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, FEATURE_REQUEST, OTHER.
            priority: one of LOW, MEDIUM, HIGH, URGENT.
            sentiment: one of POSITIVE, NEUTRAL, NEGATIVE, ANGRY.
            summary: one sentence a support engineer can read in five seconds.
            Business-tier customers reporting account-access or billing problems should
            generally be at least MEDIUM priority even if the wording is calm.
            """;

    private record ClassificationDto(String category, String priority, String sentiment, String summary) {
    }

    private final ChatClient chatClient;

    public SpringAiTicketClassifierAdapter(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public TicketClassification classify(String ticketText, Customer customer) {
        ClassificationDto dto = chatClient.prompt()
                .user(u -> u.text("Customer plan: {plan}\n\nTicket:\n{ticket}")
                        .param("plan", customer.plan())
                        .param("ticket", ticketText))
                .call()
                .entity(ClassificationDto.class);

        return new TicketClassification(
                TicketClassification.Category.valueOf(dto.category()),
                TicketClassification.Priority.valueOf(dto.priority()),
                TicketClassification.Sentiment.valueOf(dto.sentiment()),
                dto.summary());
    }
}
