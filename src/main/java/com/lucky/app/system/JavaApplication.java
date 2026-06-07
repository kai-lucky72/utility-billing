package com.lucky.app.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Spring Boot entry point for the Utility Billing System. Enables scheduling (overdue-bill sweep,
 * token cleanup) and method-level security so {@code @PreAuthorize} role checks on controllers apply.
 */
@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity
public class JavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaApplication.class, args);
    }
}
