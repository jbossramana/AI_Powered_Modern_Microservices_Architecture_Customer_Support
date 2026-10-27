package com.acmesupport.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 5: a genuine multi-step workflow instead of one ChatClient call. A ticket goes
 * through triage, then branches: urgent+negative tickets go straight to a human queue,
 * everything else goes through knowledge retrieval and gets a drafted reply. Spring AI
 * still does all the LLM calling (inside the graph's nodes) - LangGraph4j is only
 * responsible for the shape of the workflow: which step runs next, and why.
 */
@SpringBootApplication
public class OrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrationApplication.class, args);
    }
}
