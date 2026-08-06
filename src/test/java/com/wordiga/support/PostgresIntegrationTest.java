package com.wordiga.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-secret-key-for-wordiga-integration-tests-1234567890");
        registry.add("tourism.api.service-key", () -> "test-service-key");
        registry.add("tourism.api.service-key", () -> "test-service-key");
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test");
        registry.add("spring.security.oauth2.client.registration.kakao.client-id", () -> "test");
        registry.add("spring.security.oauth2.client.registration.kakao.client-secret", () -> "test");
    }
}
