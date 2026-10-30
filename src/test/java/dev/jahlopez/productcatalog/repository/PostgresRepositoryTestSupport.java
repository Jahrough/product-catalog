package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.integration.SharedTestContainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Provides the common PostgreSQL connection properties for repository slice tests.
 */
abstract class PostgresRepositoryTestSupport {

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> SharedTestContainers.postgres().getJdbcUrl());
        registry.add("spring.datasource.username", () -> SharedTestContainers.postgres().getUsername());
        registry.add("spring.datasource.password", () -> SharedTestContainers.postgres().getPassword());
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", () -> SharedTestContainers.postgres().getJdbcUrl());
        registry.add("spring.flyway.user", () -> SharedTestContainers.postgres().getUsername());
        registry.add("spring.flyway.password", () -> SharedTestContainers.postgres().getPassword());
        registry.add("spring.flyway.driver-class-name", () -> "org.postgresql.Driver");
    }
}
