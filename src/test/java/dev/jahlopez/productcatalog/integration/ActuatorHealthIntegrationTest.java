
package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Actuator health checks, Kubernetes health probes,
 * and Prometheus metrics using PostgreSQL and Redis Testcontainers.
 */
@SpringBootTest(properties = {
        "management.endpoint.health.show-components=always",
        "management.endpoint.health.show-details=always"
})
@AutoConfigureMockMvc
@Testcontainers
class ActuatorHealthIntegrationTest {

    /*
     * PostgreSQL backs the application's relational persistence layer.
     * Testcontainers assigns a host port and creates the test database.
     */
    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    /*
     * Redis supports the application's cache and its Actuator health check.
     */
    @Container
    static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /**
     * Overrides application connection settings with endpoints provided
     * by the running containers. Container ports may be dynamically mapped,
     * so tests should use the connection details exposed by Testcontainers
     * rather than hard-coded localhost ports.
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL connection and migrations.
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        // Redis connection.
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should report UP when application health indicators are healthy")
    void shouldReturnHealthStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
               .andDo(print())
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"))
               .andExpect(jsonPath("$.components.db.status").value("UP"))
               .andExpect(jsonPath("$.components.redis.status").value("UP"));
    }

    @Test
    @DisplayName("Should expose Prometheus metrics in the expected text format")
    void shouldExposePrometheusMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
               .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    @DisplayName("Should report UP for Kubernetes liveness and readiness probes")
    void shouldExposeKubernetesProbes() throws Exception {
        // Liveness indicates whether the application process is functioning.
        mockMvc.perform(get("/actuator/health/liveness"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"));

        // Readiness indicates whether the application is ready to receive traffic.
        mockMvc.perform(get("/actuator/health/readiness"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"));
    }
}