package com.acmesupport.cleanarch.adapter.in.web;

import com.acmesupport.cleanarch.application.port.in.DraftResponseUseCase;
import com.acmesupport.cleanarch.application.port.in.DraftResponseUseCase.DraftCommand;
import com.acmesupport.cleanarch.application.port.in.ReviewDraftUseCase;
import com.acmesupport.cleanarch.domain.model.DraftResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApprovalController {

    private final DraftResponseUseCase draftResponseUseCase;
    private final ReviewDraftUseCase reviewDraftUseCase;

    // Two constructor-injected interfaces, not one concrete DraftResponseService -
    // this class has no way to know (or care) that the same object satisfies both.
    public ApprovalController(DraftResponseUseCase draftResponseUseCase, ReviewDraftUseCase reviewDraftUseCase) {
        this.draftResponseUseCase = draftResponseUseCase;
        this.reviewDraftUseCase = reviewDraftUseCase;
    }

    public record DraftHttpRequest(@NotBlank String ticketId, @NotBlank String customerId, @NotBlank String ticketText) {
    }

    public record ApproveHttpRequest(@NotBlank String reviewer, String editedText) {
    }

    public record RejectHttpRequest(@NotBlank String reviewer, @NotBlank String reason) {
    }

    @PostMapping("/tickets/draft")
    public DraftResponse draft(@RequestBody DraftHttpRequest request) {
        return draftResponseUseCase.draft(new DraftCommand(request.ticketId(), request.customerId(), request.ticketText()));
    }

    @PostMapping("/approvals/{id}/approve")
    public DraftResponse approve(@PathVariable String id, @RequestBody ApproveHttpRequest request) {
        return reviewDraftUseCase.approve(id, request.reviewer(), request.editedText());
    }

    @PostMapping("/approvals/{id}/reject")
    public DraftResponse reject(@PathVariable String id, @RequestBody RejectHttpRequest request) {
        return reviewDraftUseCase.reject(id, request.reviewer(), request.reason());
    }

    @PostMapping("/approvals/{id}/send")
    public DraftResponse send(@PathVariable String id) {
        return reviewDraftUseCase.send(id);
    }
}
