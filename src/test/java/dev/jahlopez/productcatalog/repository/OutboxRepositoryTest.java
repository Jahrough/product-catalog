package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.OutboxEvent;
import dev.jahlopez.productcatalog.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data JPA test suite for {@link OutboxRepository}.
 * Uses Testcontainers and PostgreSQL to verify outbox table persistence and query operations.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class OutboxRepositoryTest extends PostgresRepositoryTestSupport {

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
                .eventId(java.util.UUID.randomUUID())
                .aggregateType("product")
                .aggregateId("123")
                .eventType("ProductCreated")
                .payload("{\"eventId\":\"test-uuid\",\"sku\":\"PROD-TEST-01\",\"version\":1}")
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
        assertThat(fetched.getAggregateType()).isEqualTo("product");
        assertThat(fetched.getAggregateId()).isEqualTo("123");
        assertThat(fetched.getEventType()).isEqualTo("ProductCreated");
        assertThat(fetched.isProcessed()).isFalse();
        assertThat(fetched.getPayload()).contains("PROD-TEST-01");
    }
}