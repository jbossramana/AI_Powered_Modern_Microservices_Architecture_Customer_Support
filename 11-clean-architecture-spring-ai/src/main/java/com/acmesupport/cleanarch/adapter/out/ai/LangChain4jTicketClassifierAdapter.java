package com.acmesupport.cleanarch.adapter.out.ai;

import com.acmesupport.cleanarch.application.port.out.TicketClassifierPort;
import com.acmesupport.cleanarch.domain.model.Customer;
import com.acmesupport.cleanarch.domain.model.TicketClassification;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/**
 * An ALTERNATIVE outbound adapter for the exact same TicketClassifierPort, using
 * LangChain4j instead of Spring AI. This is the concrete payoff of everything else in
 * this module: this class is the ONLY thing that would need to exist to switch AI
 * frameworks for ticket classification. TriageTicketService, TicketClassifierPort, and
 * every domain type are completely unaware this class exists, let alone which
 * framework it uses internally - because they were never written against Spring AI in
 * the first place, only against TicketClassifierPort.
 *
 * <p>Deliberately NOT annotated with @Component, so it is not wired up by default -
 * SpringAiTicketClassifierAdapter is the active implementation. To actually switch to
 * this one: remove @Component from SpringAiTicketClassifierAdapter, add @Component
 * here, and provide a LangChain4j {@code dev.langchain4j.model.chat.ChatModel} bean
 * (build one with {@code OllamaChatModel.builder()...}, the same way
 * 00-chat-langchain4j does it - see that module's AssistantConfig for a template).
 * Nothing else in this codebase needs to change.
 */
public class LangChain4jTicketClassifierAdapter implements TicketClassifierPort {

    interface ClassifierAssistant {
        @SystemMessage("""
                Classify this customer support ticket.
                category: one of ACCOUNT_ACCESS, BILLING, TECHNICAL_ISSUE, FEATURE_REQUEST, OTHER.
                priority: one of LOW, MEDIUM, HIGH, URGENT.
                sentiment: one of POSITIVE, NEUTRAL, NEGATIVE, ANGRY.
                summary: one sentence.
                """)
        ClassificationDto classify(String ticketTextWithCustomerContext);

        record ClassificationDto(String category, String priority, String sentiment, String summary) {
        }
    }

    private final ClassifierAssistant assistant;

    public LangChain4jTicketClassifierAdapter(ChatModel langchain4jChatModel) {
        // AiServices.create() reads ClassifierAssistant's return type and generates the
        // JSON-schema prompting and response parsing needed to return a
        // ClassificationDto directly - the LangChain4j equivalent of Spring AI's
        // .entity(Class) used in SpringAiTicketClassifierAdapter.
        this.assistant = AiServices.create(ClassifierAssistant.class, langchain4jChatModel);
    }

    @Override
    public TicketClassification classify(String ticketText, Customer customer) {
        String promptText = "Customer plan: %s\n\nTicket:\n%s".formatted(customer.plan(), ticketText);
        ClassifierAssistant.ClassificationDto dto = assistant.classify(promptText);

        return new TicketClassification(
                TicketClassification.Category.valueOf(dto.category()),
                TicketClassification.Priority.valueOf(dto.priority()),
                TicketClassification.Sentiment.valueOf(dto.sentiment()),
                dto.summary());
    }
}
