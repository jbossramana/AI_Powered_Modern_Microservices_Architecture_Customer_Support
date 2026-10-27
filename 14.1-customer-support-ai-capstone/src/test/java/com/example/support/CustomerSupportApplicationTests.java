package com.example.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerSupportApplicationTests {
    @Test
    void contextLoads() {
        // Requires a compatible Spring AI/Ollama dependency set.
        // The test does not call the model; model calls occur only through API requests.
    }
}
