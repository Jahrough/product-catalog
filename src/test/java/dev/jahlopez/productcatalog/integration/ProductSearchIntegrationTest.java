package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
import dev.jahlopez.productcatalog.document.ProductDocument;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import dev.jahlopez.productcatalog.repository.ProductSearchRepository;
import dev.jahlopez.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Enterprise integration test suite for verifying event-driven Elasticsearch indexing and full-text search capabilities.
 * Utilizes Testcontainers to spin up isolated PostgreSQL, Redis, Kafka, and Elasticsearch nodes.
 */
@SpringBootTest
@Testcontainers
class ProductSearchIntegrationTest {

    private static final String TEST_SKU = "SRCH-HUB-01";
    private static final String TEST_PRODUCT_NAME = "Wireless Mechanical Gaming Keyboard";
    private static final String TEST_PRODUCT_DESC = "RGB backlit mechanical keyboard with tactile brown switches.";
    private static final BigDecimal TEST_PRICE = new BigDecimal("129.99");
    private static final int TEST_STOCK = 50;

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
    static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("elasticsearch:8.9.0")
    ).withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("spring.elasticsearch.connection-timeout", () -> "10s");
        registry.add("spring.elasticsearch.socket-timeout", () -> "10s");
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @BeforeEach
    void setUp() {
        // Clear relational database state
        productRepository.deleteAll();
        productRepository.flush();

        // Clear search repository state
        productSearchRepository.deleteAll();

        // Explicitly recreate Elasticsearch index and mappings for complete test isolation
        if (elasticsearchOperations.indexOps(ProductDocument.class).exists()) {
            elasticsearchOperations.indexOps(ProductDocument.class).delete();
        }
        elasticsearchOperations.indexOps(ProductDocument.class).create();
        elasticsearchOperations.indexOps(ProductDocument.class).putMapping();

        // Ensure all Kafka listener containers are fully assigned partitions before executing tests
        for (MessageListenerContainer container : endpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
    }

    @Test
    @DisplayName("Should index product via Kafka event stream and perform full-text search")
    void shouldIndexAndSearchProduct() {
        // Arrange: Build the request payload for a new product
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku(TEST_SKU);
        requestDto.setName(TEST_PRODUCT_NAME);
        requestDto.setDescription(TEST_PRODUCT_DESC);
        requestDto.setPrice(TEST_PRICE);
        requestDto.setStockQuantity(TEST_STOCK);

        // Act: Create product (triggers asynchronous Kafka event, consumed and indexed into Elasticsearch)
        ProductResponseDto created = productService.createProduct(requestDto);
        assertThat(created).isNotNull();

        // Assert: Use Awaitility to wait until the asynchronous Elasticsearch consumer indexes the document
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    long count = productSearchRepository.count();
                    assertThat(count).isGreaterThan(0);

                    Page<ProductDocument> results = productSearchRepository.findByNameContainingOrDescriptionContaining(
                            "Mechanical", "Mechanical", PageRequest.of(0, 10)
                    );
                    assertThat(results).isNotNull();
                    assertThat(results.getContent()).hasSize(1);
                    assertThat(results.getContent().get(0).getSku()).isEqualTo(TEST_SKU);
                    assertThat(results.getContent().get(0).getName()).contains(TEST_PRODUCT_NAME);
                });
    }
}