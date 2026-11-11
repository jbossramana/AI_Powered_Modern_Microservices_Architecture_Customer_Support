package com.acmesupport.chat;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the thread pool used for asynchronous chat requests.
 */
@Configuration
public class AsyncConfig {

    /**
     * Creates a dedicated thread pool for LLM calls.
     *
     * @return the executor used by asynchronous chat requests
     */
    @Bean("chatExecutor")
    public Executor chatExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        // Number of threads maintained in the pool.
        executor.setCorePoolSize(5);

        // Maximum number of concurrent worker threads.
        executor.setMaxPoolSize(10);

        // Maximum number of tasks waiting in the queue.
        executor.setQueueCapacity(100);

        // Prefix used to identify worker threads in logs.
        executor.setThreadNamePrefix("chat-");

        return executor;
    }
}
