package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data JPA test suite for {@link OutboxRepository}.
 * Uses Testcontainers and PostgreSQL to verify outbox table persistence and query operations.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save outbox event and find unprocessed events successfully")
    void shouldSaveAndFindUnprocessedOutboxEvents() {
        // Arrange: Create an unprocessed outbox event
        OutboxEvent event = OutboxEvent.builder()
                .aggregateType("PRODUCT")
                .aggregateId("123")
                .eventType("CREATED")
                .payload("{\"eventId\":\"test-uuid\",\"sku\":\"PROD-TEST-01\"}")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(event);

        // Act: Query for unprocessed events
        List<OutboxEvent> unprocessedEvents = outboxRepository.findByProcessed(false);

        // Assert: Verify retrieval and fields
        assertThat(unprocessedEvents).hasSize(1);
        OutboxEvent fetched = unprocessedEvents.get(0);
        assertThat(fetched.getId()).isNotNull();
        assertThat(fetched.getAggregateType()).isEqualTo("PRODUCT");
        assertThat(fetched.getAggregateId()).isEqualTo("123");
        assertThat(fetched.getEventType()).isEqualTo("CREATED");
        assertThat(fetched.isProcessed()).isFalse();
        assertThat(fetched.getPayload()).contains("PROD-TEST-01");
    }
}