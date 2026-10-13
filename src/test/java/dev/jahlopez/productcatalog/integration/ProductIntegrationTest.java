package dev.jahlopez.productcatalog.integration;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import dev.jahlopez.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enterprise integration test suite for the product catalog microservice.
 * Utilizes Testcontainers to spin up isolated, containerized instances of PostgreSQL
 * and Redis, guaranteeing 100% environment parity with production infrastructure
 * while verifying database persistence, Flyway migrations, and Redis caching behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductIntegrationTest {

    /**
     * Isolated PostgreSQL container for database integration testing.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    /**
     * Isolated Redis container for caching and distributed data tier testing.
     */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);

    /**
     * Dynamically registers Spring Boot datasource and Redis properties from the active Testcontainers instances.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        // Redis properties dynamically mapped from Testcontainer
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Resets database state and flushes all Redis caches before each individual test execution.
     */
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        // Clear all caches between test runs
        if (cacheManager.getCacheNames() != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                if (cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                }
            });
        }
    }

    /**
     * Verifies that product entities are correctly persisted via Spring Data JPA
     * and successfully retrieved against the PostgreSQL container database.
     */
    @Test
    void shouldSaveAndRetrieveProductFromDatabase() {
        // Arrange
        Product product = Product.builder()
                .sku("PROD-MON-34")
                .name("Ultrawide Monitor")
                .description("34-inch curved gaming monitor")
                .price(BigDecimal.valueOf(499.99))
                .stockQuantity(15)
                .build();

        // Act
        Product savedProduct = productRepository.save(product);
        List<Product> products = productRepository.findAll();

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Ultrawide Monitor");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(499.99));
    }

    /**
     * Verifies that product lookup operations are successfully cached in Redis,
     * allowing subsequent requests to bypass the database layer even if the underlying entity is removed.
     */
    @Test
    void shouldCacheProductLookupInRedis() {
        // Arrange
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-KEY-01");
        requestDto.setName("Mechanical Keyboard");
        requestDto.setDescription("Wireless RGB keyboard");
        requestDto.setPrice(BigDecimal.valueOf(129.99));
        requestDto.setStockQuantity(50);

        ProductResponseDto created = productService.createProduct(requestDto);

        // Act - First call should hit the database and populate cache
        ProductResponseDto fetchedFirst = productService.getProductById(created.getId());

        // Delete from database directly to verify subsequent fetch comes from Redis cache
        productRepository.deleteById(created.getId());

        // Act - Second call should retrieve from Redis cache, bypassing the empty database table
        ProductResponseDto fetchedFromCache = productService.getProductById(created.getId());

        // Assert
        assertThat(fetchedFromCache).isNotNull();
        assertThat(fetchedFromCache.getSku()).isEqualTo("PROD-KEY-01");
        assertThat(fetchedFromCache.getName()).isEqualTo("Mechanical Keyboard");
    }
}