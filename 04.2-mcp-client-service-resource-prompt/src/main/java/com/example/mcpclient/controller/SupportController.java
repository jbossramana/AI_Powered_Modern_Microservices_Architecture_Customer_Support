package com.example.mcpclient.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mcpclient.service.SupportAssistantService;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportAssistantService assistantService;

    public SupportController(SupportAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    public record QuestionRequest(
            @NotBlank String question) {
    }

    public record RefundDraftRequest(
            @NotBlank String customerName,
            @NotBlank String customerIssue) {
    }

    @PostMapping("/answer")
    public AnswerResponse answer(@Valid @RequestBody QuestionRequest request) {
        return new AnswerResponse(
                assistantService.answerWithTools(request.question())
        );
    }

    @PostMapping("/refund-draft")
    public AnswerResponse refundDraft(
            @Valid @RequestBody RefundDraftRequest request) {
        return new AnswerResponse(
                assistantService.draftRefundResponse(
                        request.customerName(),
                        request.customerIssue()
                )
        );
    }

    public record AnswerResponse(String answer) {
    }
}
