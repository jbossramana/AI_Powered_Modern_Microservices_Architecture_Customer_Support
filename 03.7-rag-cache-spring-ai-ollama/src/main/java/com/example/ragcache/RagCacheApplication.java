package com.example.ragcache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RagCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagCacheApplication.class, args);
    }
}
