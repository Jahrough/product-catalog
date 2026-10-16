package dev.jahlopez.productcatalog.integration;

import com.redis.testcontainers.RedisContainer;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductIntegrationTest {

    /**
     * Isolated PostgreSQL container for database integration testing.
     */
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    /**
     * Isolated Redis container for caching and distributed data tier testing.
     */
    @Container
    static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /**
     * Dynamically registers Spring Boot datasource and Redis properties from the active Testcontainers instances.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

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
        productRepository.flush();

        for (String cacheName : List.of("productsById", "productsBySku", "productsList")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    /**
     * Verifies that product entities are correctly persisted via Spring Data JPA
     * and successfully retrieved against the PostgreSQL container database.
     */
    @Test
    void shouldSaveAndRetrieveProductFromDatabase() {
        Product product = Product.builder()
                .sku("PROD-MON-34")
                .name("Ultrawide Monitor")
                .description("34-inch curved gaming monitor")
                .price(new BigDecimal("499.99"))
                .stockQuantity(15)
                .build();

        Product savedProduct = productRepository.saveAndFlush(product);
        List<Product> products = productRepository.findAll();

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getSku()).isEqualTo("PROD-MON-34");
        assertThat(products.get(0).getName()).isEqualTo("Ultrawide Monitor");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("499.99"));
    }

    /**
     * Verifies that product lookup operations are successfully cached in Redis,
     * allowing subsequent requests to bypass the database layer even if the underlying entity is removed.
     */
    @Test
    void shouldCacheProductLookupInRedis() {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-KEY-01");
        requestDto.setName("Mechanical Keyboard");
        requestDto.setDescription("Wireless RGB keyboard");
        requestDto.setPrice(new BigDecimal("129.99"));
        requestDto.setStockQuantity(50);

        ProductResponseDto created = productService.createProduct(requestDto);

        // First lookup loads the product and should populate the cache.
        ProductResponseDto fetchedFirst = productService.getProductById(created.getId());

        assertThat(fetchedFirst).isNotNull();
        assertThat(fetchedFirst.getId()).isEqualTo(created.getId());
        assertThat(fetchedFirst.getSku()).isEqualTo("PROD-KEY-01");

        // Retrieve the specific cache and entry
        Cache productCache = cacheManager.getCache("productsById");
        assertThat(productCache).as("'productsById' cache must be configured").isNotNull();

        Cache.ValueWrapper cachedEntry = productCache.get(created.getId());
        assertThat(cachedEntry).as("Product lookup should create a cache entry").isNotNull();

        assertThat(cachedEntry.get())
                .as("Cached value should deserialize to ProductResponseDto")
                .isInstanceOf(ProductResponseDto.class);

        ProductResponseDto cachedProduct = (ProductResponseDto) cachedEntry.get();

        assertThat(cachedProduct.getId()).isEqualTo(created.getId());
        assertThat(cachedProduct.getSku()).isEqualTo("PROD-KEY-01");

        // Delete directly through the repository so cache eviction
        // in ProductService.deleteProduct() is not triggered.
        productRepository.deleteById(created.getId());
        productRepository.flush();

        assertThat(productRepository.findById(created.getId())).isEmpty();

        // The cache should still supply the product despite its deletion.
        ProductResponseDto fetchedFromCache = productService.getProductById(created.getId());

        assertThat(fetchedFromCache).isNotNull();
        assertThat(fetchedFromCache.getId()).isEqualTo(created.getId());
        assertThat(fetchedFromCache.getSku()).isEqualTo("PROD-KEY-01");
        assertThat(fetchedFromCache.getName()).isEqualTo("Mechanical Keyboard");
    }
}