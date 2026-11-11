package com.example.support.api;

import com.example.support.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoController {
    private final WorkflowService workflows;

    public DemoController(WorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping("/no-agent/single-tool")
    public Map<String, Object> noAgentSingle(@Valid @RequestBody ApiModels.SingleToolRequest request) {
        return workflows.noAgentSingleTool(request.customerId());
    }

    @PostMapping("/no-agent/multiple-tools")
    public Map<String, Object> noAgentMultiple(@Valid @RequestBody ApiModels.FixedWorkflowRequest request) {
        return workflows.noAgentMultipleTools(request.ticketId(), request.customerId());
    }

    @PostMapping("/no-agent/single-tool-with-llm")
    public Map<String, Object> noAgentSingleWithLlm(@Valid @RequestBody ApiModels.SingleToolRequest request) {
        return workflows.noAgentSingleToolWithLlm(request.customerId());
    }

    @PostMapping("/no-agent/multiple-tools-with-llm")
    public Map<String, Object> noAgentMultipleWithLlm(@Valid @RequestBody ApiModels.FixedWorkflowRequest request) {
        return workflows.noAgentMultipleToolsWithLlm(request.ticketId(), request.customerId());
    }

    @PostMapping("/agent/single-tool")
    public Map<String, Object> agentSingle(@Valid @RequestBody ApiModels.QuestionRequest request) {
        return workflows.agentSingleTool(request.question());
    }

    @PostMapping("/agent/multiple-tools")
    public Map<String, Object> agentMultiple(@Valid @RequestBody ApiModels.QuestionRequest request) {
        return workflows.agentMultipleTools(request.question());
    }

    @PostMapping("/agentic/single-tool")
    public Map<String, Object> agenticSingle(@Valid @RequestBody ApiModels.AgenticRequest request) {
        return workflows.agenticSingleTool(request.goal(), request.ticketId());
    }

    @PostMapping("/agentic/multiple-tools")
    public Map<String, Object> agenticMultiple(@Valid @RequestBody ApiModels.AgenticRequest request) {
        return workflows.agenticMultipleTools(request.goal(), request.ticketId(), request.customerId());
    }

    @PostMapping("/multi-agent/support")
    public Map<String, Object> multiAgent(@Valid @RequestBody ApiModels.MultiAgentRequest request) {
        return workflows.multiAgent(request.message(), request.ticketId(), request.customerId());
    }
}
