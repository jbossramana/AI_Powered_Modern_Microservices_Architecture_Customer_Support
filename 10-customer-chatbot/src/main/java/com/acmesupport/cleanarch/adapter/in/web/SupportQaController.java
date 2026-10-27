package com.acmesupport.cleanarch.adapter.in.web;

import com.acmesupport.cleanarch.application.port.in.AnswerSupportQuestionUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupportQaController {

    private final AnswerSupportQuestionUseCase answerSupportQuestionUseCase;

    public SupportQaController(AnswerSupportQuestionUseCase answerSupportQuestionUseCase) {
        this.answerSupportQuestionUseCase = answerSupportQuestionUseCase;
    }

    public record AskRequest(@NotBlank String question) {
    }

    /**
     * POST /api/support/ask
     * {"question": "..."}
     *
     * Returns the use case's Answer record directly - the pragmatic approach (see
     * TicketController for the stricter, explicitly-mapped alternative). Answer is
     * already a minimal, intentionally-shaped record, so mapping it to yet another DTO
     * here would add a file without changing the wire format at all.
     */
    @PostMapping("/api/support/ask")
    public AnswerSupportQuestionUseCase.Answer ask(@RequestBody AskRequest request) {
        return answerSupportQuestionUseCase.answer(request.question());
    }
}
