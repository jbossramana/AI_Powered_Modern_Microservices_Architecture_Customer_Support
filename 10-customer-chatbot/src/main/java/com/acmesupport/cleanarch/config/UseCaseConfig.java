package com.acmesupport.cleanarch.config;

import com.acmesupport.cleanarch.application.port.in.AnswerSupportQuestionUseCase;
import com.acmesupport.cleanarch.application.port.in.GetCustomerDetailsUseCase;
import com.acmesupport.cleanarch.application.port.in.TriageTicketUseCase;
import com.acmesupport.cleanarch.application.port.out.CustomerRepositoryPort;
import com.acmesupport.cleanarch.application.port.out.DraftResponseRepositoryPort;
import com.acmesupport.cleanarch.application.port.out.KnowledgeBaseSearchPort;
import com.acmesupport.cleanarch.application.port.out.ResponseGeneratorPort;
import com.acmesupport.cleanarch.application.port.out.TicketClassifierPort;
import com.acmesupport.cleanarch.application.port.out.TicketRepositoryPort;
import com.acmesupport.cleanarch.application.service.AnswerSupportQuestionService;
import com.acmesupport.cleanarch.application.service.DraftResponseService;
import com.acmesupport.cleanarch.application.service.GetCustomerDetailsService;
import com.acmesupport.cleanarch.application.service.TriageTicketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The use-case classes in application/service are deliberately plain Java - no
 * {@code @Service}, no {@code @Component}, no Spring imports at all. That's a
 * "purist" design choice, not an oversight: it keeps the application layer honestly
 * framework-free rather than merely framework-light, and this class is where that
 * tradeoff gets paid for, in one place, instead of scattered across every use case as
 * a stereotype annotation.
 *
 * <p>Most real Spring teams take the more "pragmatic" middle ground instead: put
 * {@code @Service} directly on TriageTicketService and let component scanning wire it,
 * accepting one import of {@code org.springframework.stereotype.Service} into the
 * application layer as a worthwhile trade for less boilerplate here. Both are
 * legitimate; this module takes the stricter path specifically so the Dependency Rule
 * is airtight and easy to verify by inspection (grep application/ for "springframework"
 * and get nothing) rather than "mostly true."
 */
@Configuration
public class UseCaseConfig {

    @Bean
    TriageTicketUseCase triageTicketUseCase(
            TicketClassifierPort classifier, CustomerRepositoryPort customers, TicketRepositoryPort tickets) {
        return new TriageTicketService(classifier, customers, tickets);
    }

    @Bean
    AnswerSupportQuestionUseCase answerSupportQuestionUseCase(
            KnowledgeBaseSearchPort knowledgeBase, ResponseGeneratorPort responseGenerator) {
        return new AnswerSupportQuestionService(knowledgeBase, responseGenerator);
    }

    @Bean
    GetCustomerDetailsUseCase getCustomerDetailsUseCase(CustomerRepositoryPort customers) {
        return new GetCustomerDetailsService(customers);
    }

    /**
     * DraftResponseService implements both DraftResponseUseCase and ReviewDraftUseCase;
     * returning the concrete type from this @Bean method lets Spring satisfy both
     * interfaces' injection points (see ApprovalController's two constructor
     * parameters) from this one instance.
     */
    @Bean
    DraftResponseService draftResponseService(
            ResponseGeneratorPort responseGenerator, KnowledgeBaseSearchPort knowledgeBase, DraftResponseRepositoryPort drafts) {
        return new DraftResponseService(responseGenerator, knowledgeBase, drafts);
    }
}
