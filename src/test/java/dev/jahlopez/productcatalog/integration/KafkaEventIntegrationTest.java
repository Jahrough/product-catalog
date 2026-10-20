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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Enterprise integration test suite for verifying asynchronous event publishing to Apache Kafka.
 * <p>Utilizes Testcontainers to spin up isolated instances of PostgreSQL, Redis, and Kafka,
 * capturing published events via a dedicated test Kafka listener bean and Awaitility synchronization.</p>
 */
@SpringBootTest
@Testcontainers
class KafkaEventIntegrationTest {

    // Environment-agnostic Docker socket discovery fallback for macOS / Linux environments
    static {
        if (System.getenv("DOCKER_HOST") == null) {
            java.io.File standardSocket = new java.io.File("/var/run/docker.sock");
            java.io.File desktopSocket = new java.io.File(System.getProperty("user.home") + "/.docker/run/docker.sock");

            if (desktopSocket.exists()) {
                System.setProperty("docker.host", "unix://" + desktopSocket.getAbsolutePath());
            } else if (standardSocket.exists()) {
                System.setProperty("docker.host", "unix:///var/run/docker.sock");
            }
        }
    }

    /** Isolated PostgreSQL container instance (Singleton pattern) for database persistence testing. */
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    /** Isolated Redis container instance (Singleton pattern) for caching layer testing. */
    @Container
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /** Isolated Apache Kafka container instance (Singleton pattern) for event-driven messaging testing. */
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    /**
     * Dynamically registers test container connection properties into the Spring Environment,
     * overriding application configuration parameters at runtime.
     *
     * @registry the dynamic property registry
     */
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL and Flyway migration configurations
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        // Redis cache connection configurations
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Apache Kafka broker and consumer configuration overrides
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestKafkaListener testKafkaListener;

    /**
     * Resets database state and clears captured Kafka messages before each individual test execution
     * to ensure strict test isolation.
     */
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.flush();
        testKafkaListener.clearQueue();
    }

    @Test
    @DisplayName("Should publish product created event to Kafka container topic")
    void shouldPublishProductCreatedEvent() {
        // Arrange: Prepare valid product request payload
        ProductRequestDto requestDto = createSampleRequest("KAFKA-PROD-01", "Stream Deck", new BigDecimal("149.99"));

        // Act: Execute business logic to create product and trigger asynchronous event publication
        ProductResponseDto created = productService.createProduct(requestDto);

        // Assert: Use Awaitility to poll asynchronously until the message is intercepted by the test consumer
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    ConsumerRecord<String, Object> receivedMessage = testKafkaListener.getMessages().peek();
                    assertThat(receivedMessage).as("Kafka event must be published").isNotNull();
                    assertThat(receivedMessage.key()).isEqualTo(requestDto.getSku());
                    assertThat(receivedMessage.value()).isNotNull();
                });
    }

    @Test
    @DisplayName("Should publish product deleted event to Kafka container topic")
    void shouldPublishProductDeletedEvent() {
        // Arrange: Create initial product record to establish state for deletion
        ProductRequestDto initialRequest = createSampleRequest("KAFKA-PROD-03", "Studio Microphone", new BigDecimal("99.99"));
        ProductResponseDto created = productService.createProduct(initialRequest);

        // Wait for the initial creation event to clear from the queue so we can assert on deletion explicitly
        await().atMost(Duration.ofSeconds(10))
                .until(() -> !testKafkaListener.getMessages().isEmpty());
        testKafkaListener.clearQueue();

        // Act: Delete the product to trigger the product deletion event
        productService.deleteProduct(created.getId());

        // Assert: Use Awaitility to wait for deletion event publication and verify key matching
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    ConsumerRecord<String, Object> receivedMessage = testKafkaListener.getMessages().peek();
                    assertThat(receivedMessage).as("Kafka delete event must be published").isNotNull();
                    assertThat(receivedMessage.key()).isEqualTo(initialRequest.getSku());
                    assertThat(receivedMessage.value()).isNotNull();
                });
    }

    /**
     * Helper factory method to build a fully valid {@link ProductRequestDto} baseline for testing.
     *
     * @param sku   the unique stock keeping unit identifier
     * @param name  the name of the product
     * @param price the monetary price of the product
     * @return configured product request DTO instance
     */
    private ProductRequestDto createSampleRequest(String sku, String name, BigDecimal price) {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku(sku);
        requestDto.setName(name);
        requestDto.setDescription("High performance broadcasting peripheral");
        requestDto.setPrice(price);
        requestDto.setStockQuantity(25);
        return requestDto;
    }

    /**
     * Test-specific configuration container defining beans required exclusively for integration verification.
     */
    @TestConfiguration
    static class TestKafkaConfiguration {

        @Bean
        public TestKafkaListener testKafkaListener() {
            return new TestKafkaListener();
        }
    }

    /**
     * Dedicated test Kafka listener component that intercepts published records on the `product-events` topic
     * and queues them in a thread-safe structure for assertions.
     */
    static class TestKafkaListener {
        private final BlockingQueue<ConsumerRecord<String, Object>> messages = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "product-events", groupId = "test-assertion-group")
        void listen(ConsumerRecord<String, Object> record) {
            messages.add(record);
        }

        /**
         * Retrieves the thread-safe queue of intercepted Kafka consumer records.
         *
         * @return blocking queue containing consumer records
         */
        public BlockingQueue<ConsumerRecord<String, Object>> getMessages() {
            return messages;
        }

        /**
         * Clears all recorded messages from the queue.
         */
        public void clearQueue() {
            messages.clear();
        }
    }
}