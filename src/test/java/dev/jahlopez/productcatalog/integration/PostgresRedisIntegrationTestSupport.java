package dev.jahlopez.productcatalog.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Shares one PostgreSQL/Redis runtime and Spring application context configuration
 * across integration test classes that exercise the full application.
 */
@SpringBootTest(properties = {
        "management.endpoint.health.show-components=always",
        "management.endpoint.health.show-details=always"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class PostgresRedisIntegrationTestSupport {

    @DynamicPropertySource
    static void configureContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> SharedTestContainers.postgres().getJdbcUrl());
        registry.add("spring.datasource.username", () -> SharedTestContainers.postgres().getUsername());
        registry.add("spring.datasource.password", () -> SharedTestContainers.postgres().getPassword());
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.data.redis.host", () -> SharedTestContainers.redis().getHost());
        registry.add("spring.data.redis.port", () -> SharedTestContainers.redis().getMappedPort(6379));
    }
}
