package com.acmesupport.cleanarch.application.port.in;

import com.acmesupport.cleanarch.domain.model.DraftResponse;

/**
 * Deliberately a separate interface from DraftResponseUseCase (Interface Segregation):
 * "draft a reply" and "review/send a reply" are different responsibilities with
 * different callers in a real system (an AI pipeline triggers the first; a human
 * support engineer's console triggers the second). DraftResponseService happens to
 * implement both, but callers depend on only the one they need.
 */
public interface ReviewDraftUseCase {

    DraftResponse approve(String draftId, String reviewer, String editedTextOrNull);

    DraftResponse reject(String draftId, String reviewer, String reason);

    DraftResponse send(String draftId);
}
