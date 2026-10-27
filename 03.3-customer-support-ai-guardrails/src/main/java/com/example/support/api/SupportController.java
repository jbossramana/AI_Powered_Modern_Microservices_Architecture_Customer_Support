package com.example.support.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {
    private final SupportService service;
    public SupportController(SupportService service) { this.service = service; }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return service.ask(request.message());
    }
}
