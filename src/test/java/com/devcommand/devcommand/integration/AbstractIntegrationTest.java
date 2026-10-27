package com.devcommand.devcommand.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String dbUrl = System.getenv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/devcommand_test?sslmode=disable");
        String dbUser = System.getenv().getOrDefault("DATABASE_USERNAME", "postgres");
        String dbPass = System.getenv().getOrDefault("DATABASE_PASSWORD", System.getenv().getOrDefault("POSTGRES_PASSWORD", ""));

        registry.add("spring.datasource.url", () -> dbUrl);
        registry.add("spring.datasource.username", () -> dbUser);
        registry.add("spring.datasource.password", () -> dbPass);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add(
                "devcommand.jwt.secret",
                () -> "change-this-test-secret-key-that-is-long-enough-for-hs256"
        );

        registry.add(
                "devcommand.jwt.expiration-ms",
                () -> "3600000"
        );
    }
}