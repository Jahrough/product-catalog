package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.OutboxEvent;
import dev.jahlopez.productcatalog.repository.OutboxRepository;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import dev.jahlopez.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ProductOutboxIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        productRepository.deleteAll();
        productRepository.flush();
    }

    @Test
    @DisplayName("Should transactionally persist a product-created event in the outbox table")
    void shouldPersistProductCreatedEventInOutbox() {
        ProductRequestDto request = createSampleRequest("OUTBOX-PROD-01", "Stream Deck", new BigDecimal("149.99"));

        ProductResponseDto created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSku()).isEqualTo(request.getSku());

        List<OutboxEvent> outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEvent event = outboxEvents.get(0);
        assertThat(event.getAggregateType()).isEqualTo("product");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(created.getId().toString());
        assertThat(event.getEventType()).isEqualTo("ProductCreated");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getPayload()).contains("Stream Deck");
        assertThat(event.getPayload()).contains("\"version\":1");
        assertThat(created.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should reject outbox payloads with non-positive versions")
    void shouldRejectOutboxPayloadWithNonPositiveVersion() {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateType("product")
                .aggregateId("1")
                .eventType("ProductCreated")
                .payload("{\"version\":0}")
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> outboxRepository.saveAndFlush(event))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should emit full lifecycle with versions 1 (create), 2 (update), 3 (delete)")
    void shouldEmitMonotonicVersionsAcrossLifecycle() {
        ProductResponseDto created = productService.createProduct(
                createSampleRequest("OUTBOX-PROD-03", "Webcam", new BigDecimal("79.99")));
        ProductResponseDto updated = productService.updateProduct(created.getId(),
                createSampleRequest("OUTBOX-PROD-03", "Webcam Pro", new BigDecimal("89.99")));
        assertThat(updated.getVersion()).isEqualTo(2L);

        productService.deleteProduct(created.getId());

        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactly("ProductCreated", "ProductUpdated", "ProductDeleted");
        assertThat(events.get(0).getPayload()).contains("\"version\":1");
        assertThat(events.get(1).getPayload()).contains("\"version\":2");
        assertThat(events.get(2).getPayload()).contains("\"version\":3").contains("DELETED");
    }

    @Test
    @DisplayName("Should transactionally persist a product-deleted event in the outbox table")
    void shouldPersistProductDeletedEventInOutbox() {
        ProductRequestDto request = createSampleRequest("OUTBOX-PROD-02", "Studio Microphone", new BigDecimal("99.99"));
        ProductResponseDto created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();

        // Clear outbox events from creation to isolate the deletion test
        outboxRepository.deleteAll();

        productService.deleteProduct(created.getId());

        List<OutboxEvent> outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEvent event = outboxEvents.get(0);
        assertThat(event.getAggregateType()).isEqualTo("product");
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getAggregateId()).isEqualTo(created.getId().toString());
        assertThat(event.getEventType()).isEqualTo("ProductDeleted");
        assertThat(event.isProcessed()).isFalse();
    }

    private ProductRequestDto createSampleRequest(String sku, String name, BigDecimal price) {
        ProductRequestDto request = new ProductRequestDto();
        request.setSku(sku);
        request.setName(name);
        request.setDescription("High-performance broadcasting peripheral");
        request.setPrice(price);
        request.setStockQuantity(25);

        return request;
    }
}