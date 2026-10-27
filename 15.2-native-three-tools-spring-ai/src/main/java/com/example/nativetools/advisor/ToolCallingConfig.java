
package com.example.nativetools.advisor;

import com.example.nativetools.advisor.AuditingToolCallingAdvisor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;

@Configuration
public class ToolCallingConfig {

    @Bean
    public ToolExecutionEligibilityChecker toolExecutionEligibilityChecker() {
        return chatResponse ->
                chatResponse != null && chatResponse.hasToolCalls();
    }

    @Bean
    public AuditingToolCallingAdvisor auditingToolCallingAdvisor(
            ToolCallingManager manager,
            ToolExecutionEligibilityChecker checker) {

        return new AuditingToolCallingAdvisor(
                manager,
                checker,
                0,
                true
        );
    }
}
