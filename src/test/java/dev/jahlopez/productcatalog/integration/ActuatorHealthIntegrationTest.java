
package dev.jahlopez.productcatalog.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Boot Actuator health endpoints,
 * Kubernetes health probes, and Prometheus metrics.
 *
 * <p>Tests use the shared PostgreSQL and Redis containers and application
 * context provided by {@link PostgresRedisIntegrationTestSupport}.</p>
 */
class ActuatorHealthIntegrationTest extends PostgresRedisIntegrationTestSupport {

    private static final String HEALTH_ENDPOINT = "/actuator/health";
    private static final String LIVENESS_ENDPOINT = "/actuator/health/liveness";
    private static final String READINESS_ENDPOINT = "/actuator/health/readiness";
    private static final String PROMETHEUS_ENDPOINT = "/actuator/prometheus";

    private static final String EXPECTED_HEALTH_STATUS = "UP";

    @Autowired
    private MockMvc mockMvc;

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