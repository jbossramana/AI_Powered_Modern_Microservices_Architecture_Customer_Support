package com.example.support.controller;

import com.example.support.model.TicketRequest;
import com.example.support.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
	private final TicketService service;

	public TicketController(TicketService service) {
		this.service = service;
	}

	@PostMapping("/process")
	public TicketService.TicketResult process(@Valid @RequestBody TicketRequest request) {
		return service.process(request);
	}
}
