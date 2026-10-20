package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import dev.jahlopez.productcatalog.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@Import(KafkaEventIntegrationTest.TestKafkaConfiguration.class)
class KafkaEventIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");

    @Container
    static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.flush();
        testKafkaListener.clearQueue();
    }

    @Test
    @DisplayName("Should publish a product-created event to Kafka")
    void shouldPublishProductCreatedEvent() {
        ProductRequestDto request = createSampleRequest("KAFKA-PROD-01", "Stream Deck", new BigDecimal("149.99"));

        ProductResponseDto created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSku()).isEqualTo(request.getSku());

        ConsumerRecord<String, Object> record = awaitRecordForSku(request.getSku());

        assertThat(record.key()).isEqualTo(request.getSku());
        assertThat(record.value()).as("The Kafka event payload must not be null").isNotNull();
    }

    @Test
    @DisplayName("Should publish a product-deleted event to Kafka")
    void shouldPublishProductDeletedEvent() {
        ProductRequestDto request = createSampleRequest("KAFKA-PROD-03", "Studio Microphone", new BigDecimal("99.99"));
        ProductResponseDto created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();

        // Consume the creation event before triggering deletion.
        ConsumerRecord<String, Object> creationRecord = awaitRecordForSku(request.getSku());
        assertThat(creationRecord.value()).isNotNull();

        productService.deleteProduct(created.getId());

        // Wait for the next matching event, which should be the deletion event.
        ConsumerRecord<String, Object> deletionRecord = awaitRecordForSku(request.getSku());

        assertThat(deletionRecord.key()).isEqualTo(request.getSku());
        assertThat(deletionRecord.value()).as("The product-deleted event payload must not be null").isNotNull();
    }

    private ConsumerRecord<String, Object> awaitRecordForSku(String sku) {
        AtomicReference<ConsumerRecord<String, Object>> matchingRecord = new AtomicReference<>();

        await().atMost(EVENT_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .until(() -> {
                    ConsumerRecord<String, Object> record;

                    while ((record = testKafkaListener.pollMessage()) != null) {
                        if (sku.equals(record.key())) {
                            matchingRecord.set(record);
                            return true;
                        }
                    }

                    return false;
                });

        return matchingRecord.get();
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

    @TestConfiguration(proxyBeanMethods = false)
    static class TestKafkaConfiguration {

        @Bean
        TestKafkaListener testKafkaListener() {
            return new TestKafkaListener();
        }
    }

    static class TestKafkaListener {

        private final BlockingQueue<ConsumerRecord<String, Object>> messages = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "product-events", groupId = "test-assertion-group")
        void listen(ConsumerRecord<String, Object> record) {
            messages.add(record);
        }

        ConsumerRecord<String, Object> pollMessage() {
            return messages.poll();
        }

        void clearQueue() {
            messages.clear();
        }
    }
}
