
package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Boot Actuator health endpoints,
 * Kubernetes health probes, and Prometheus metrics.
 *
 * <p>Tests use real PostgreSQL and Redis instances managed by Testcontainers.
 * The postgres-test profile enables database integration and schema validation.</p>
 */
@SpringBootTest(properties = {
        "management.endpoint.health.show-components=always",
        "management.endpoint.health.show-details=always"
})
@AutoConfigureMockMvc
@ActiveProfiles("postgres-test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActuatorHealthIntegrationTest {

    private static final String HEALTH_ENDPOINT = "/actuator/health";
    private static final String LIVENESS_ENDPOINT = "/actuator/health/liveness";
    private static final String READINESS_ENDPOINT = "/actuator/health/readiness";
    private static final String PROMETHEUS_ENDPOINT = "/actuator/prometheus";

    private static final String EXPECTED_HEALTH_STATUS = "UP";

    /**
     * PostgreSQL instance used for database connectivity and schema
     * initialization tests.
     */
    @Container
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Redis instance used to verify the application's Redis health indicator.
     */
    @Container
    static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Autowired
    private MockMvc mockMvc;

    /**
     * Supplies connection properties from the running Testcontainers
     * instances before the Spring application context is initialized.
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("Reports UP when PostgreSQL and Redis are healthy")
    void shouldReturnHealthStatusUp() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value(EXPECTED_HEALTH_STATUS))
               .andExpect(jsonPath("$.components.db.status").value(EXPECTED_HEALTH_STATUS))
               .andExpect(jsonPath("$.components.redis.status").value(EXPECTED_HEALTH_STATUS));
    }

    @Test
    @DisplayName("Exposes JVM memory metrics through Prometheus")
    void shouldExposePrometheusMetricsEndpoint() throws Exception {
        mockMvc.perform(get(PROMETHEUS_ENDPOINT))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
               .andExpect(content().string(
                       containsString("jvm_memory_used_bytes")
               ));
    }

    @Test
    @DisplayName("Reports UP for Kubernetes liveness and readiness probes")
    void shouldExposeKubernetesProbes() throws Exception {
        assertHealthEndpointUp(LIVENESS_ENDPOINT);
        assertHealthEndpointUp(READINESS_ENDPOINT);
    }

    /**
     * Asserts that an Actuator health endpoint returns HTTP 200 and UP.
     */
    private void assertHealthEndpointUp(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value(EXPECTED_HEALTH_STATUS));
    }
}