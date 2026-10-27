package com.acmesupport.cleanarch.application.service;

import com.acmesupport.cleanarch.application.port.in.DraftResponseUseCase;
import com.acmesupport.cleanarch.application.port.in.ReviewDraftUseCase;
import com.acmesupport.cleanarch.application.port.out.DraftResponseRepositoryPort;
import com.acmesupport.cleanarch.application.port.out.KnowledgeBaseSearchPort;
import com.acmesupport.cleanarch.application.port.out.ResponseGeneratorPort;
import com.acmesupport.cleanarch.domain.exception.DraftNotFoundException;
import com.acmesupport.cleanarch.domain.model.DraftResponse;
import com.acmesupport.cleanarch.domain.model.KnowledgeArticle;

import java.util.List;
import java.util.UUID;

public class DraftResponseService implements DraftResponseUseCase, ReviewDraftUseCase {

    private final ResponseGeneratorPort responseGenerator;
    private final KnowledgeBaseSearchPort knowledgeBase;
    private final DraftResponseRepositoryPort drafts;

    public DraftResponseService(
            ResponseGeneratorPort responseGenerator, KnowledgeBaseSearchPort knowledgeBase, DraftResponseRepositoryPort drafts) {
        this.responseGenerator = responseGenerator;
        this.knowledgeBase = knowledgeBase;
        this.drafts = drafts;
    }

    @Override
    public DraftResponse draft(DraftCommand command) {
        List<KnowledgeArticle> context = knowledgeBase.search(command.ticketText(), 4);
        String body = responseGenerator.generateReply(command.ticketText(), context);
        DraftResponse draft = DraftResponse.newDraft(UUID.randomUUID().toString(), command.ticketId(), body);
        return drafts.save(draft);
    }

    @Override
    public DraftResponse approve(String draftId, String reviewer, String editedTextOrNull) {
        DraftResponse current = find(draftId);
        String finalText = (editedTextOrNull == null || editedTextOrNull.isBlank()) ? current.bodyText() : editedTextOrNull;
        return drafts.save(current.approve(reviewer, finalText));
    }

    @Override
    public DraftResponse reject(String draftId, String reviewer, String reason) {
        return drafts.save(find(draftId).reject(reviewer, reason));
    }

    /**
     * The enforcement point: nothing can move PENDING -> SENT directly, and nothing
     * outside this method decides whether a send is allowed. Exactly the same
     * guarantee as DraftAndApprovalService.send() in 06-human-in-the-loop-spring-ai -
     * only now it's unreachable from a REST call without going through this exact
     * check, since ApprovalController has no other way to trigger a send.
     */
    @Override
    public DraftResponse send(String draftId) {
        DraftResponse current = find(draftId);
        if (current.status() != DraftResponse.ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot send draft %s: status is %s, not APPROVED".formatted(draftId, current.status()));
        }
        return drafts.save(current.markSent());
    }

    private DraftResponse find(String draftId) {
        return drafts.findById(draftId).orElseThrow(() -> new DraftNotFoundException(draftId));
    }
}
