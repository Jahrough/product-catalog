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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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