package com.example.support.security;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorator that validates a tool request immediately before delegation.
 */
public class GuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ToolExecutionPolicy policy;

    public GuardedToolCallback(
            ToolCallback delegate,
            ToolExecutionPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        policy.validate(getToolDefinition().name(), toolInput);
        System.out.println("POLICY APPROVED");
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        policy.validate(getToolDefinition().name(), toolInput);
        System.out.println("POLICY APPROVED");
        return delegate.call(toolInput, toolContext);
    }
}
