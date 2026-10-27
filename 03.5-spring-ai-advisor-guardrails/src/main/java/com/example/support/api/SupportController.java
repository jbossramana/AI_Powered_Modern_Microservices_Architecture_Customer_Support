package com.example.support.api;

import com.example.support.guardrail.InputGuardrail;
import com.example.support.guardrail.OutputGuardrail;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final ChatClient chatClient;
    private final InputGuardrail inputGuardrail;
    private final OutputGuardrail outputGuardrail;

    public SupportController(
            ChatClient supportChatClient,
            InputGuardrail inputGuardrail,
            OutputGuardrail outputGuardrail) {
        this.chatClient = supportChatClient;
        this.inputGuardrail = inputGuardrail;
        this.outputGuardrail = outputGuardrail;
    }

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        // BEFORE LLM: application-level input guardrail
        inputGuardrail.validate(request.question());

        // ChatClient runs configured Advisors, including SimpleLoggerAdvisor.
        String rawAnswer = chatClient.prompt()
                .user(request.question())
                .call()
                .content();

        // AFTER LLM: inspect the generated answer before returning it.
        String safeAnswer = outputGuardrail.validateAndSanitize(rawAnswer);

        return new AskResponse(safeAnswer,
                "Input and output checks passed. This demo is not a complete security system.");
    }
}
