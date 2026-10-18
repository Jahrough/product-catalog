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

import java.io.File;
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

    // Automatically detect Docker Desktop socket on macOS if DOCKER_HOST is not set
    static {
        if (System.getenv("DOCKER_HOST") == null && System.getProperty("docker.host") == null) {
            String macSocketPath = System.getProperty("user.home") + "/.docker/run/docker.sock";
            if (new File(macSocketPath).exists()) {
                System.setProperty("docker.host", "unix://" + macSocketPath);
            }
        }
    }

    /**
     * Isolated PostgreSQL container instance for integration testing.
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
     * @param registry the dynamic property registry provided by Spring Test framework
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL datasource properties mapped from Testcontainers
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        // Redis host and dynamic port mapping from Testcontainers
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
        // Purge database table content to guarantee test isolation
        productRepository.deleteAll();
        productRepository.flush();

        // Evict all entries across all application cache regions
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
        // Arrange: Instantiate a product entity
        Product product = Product.builder()
                .sku("PROD-MON-34")
                .name("Ultrawide Monitor")
                .description("34-inch curved gaming monitor")
                .price(new BigDecimal("499.99"))
                .stockQuantity(15)
                .build();

        // Act: Persist the product directly to PostgreSQL and fetch all records
        Product savedProduct = productRepository.saveAndFlush(product);
        List<Product> products = productRepository.findAll();

        // Assert: Ensure generated audit fields and primary key are present
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();

        // Assert: Verify persisted field values match the expected state
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
        // Arrange: Prepare request DTO for product creation
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-KEY-01");
        requestDto.setName("Mechanical Keyboard");
        requestDto.setDescription("Wireless RGB keyboard");
        requestDto.setPrice(new BigDecimal("129.99"));
        requestDto.setStockQuantity(50);

        // Create product via service
        ProductResponseDto created = productService.createProduct(requestDto);

        // Act: First lookup loads the product from DB and populates the Redis cache
        ProductResponseDto fetchedFirst = productService.getProductById(created.getId());

        // Assert: Check primary fetch returned correct data
        assertThat(fetchedFirst).isNotNull();
        assertThat(fetchedFirst.getId()).isEqualTo(created.getId());
        assertThat(fetchedFirst.getSku()).isEqualTo("PROD-KEY-01");

        // Inspect Redis cache instance directly to verify caching mechanism
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

        // Act: Delete record directly through repository so cache eviction in ProductService.deleteProduct() is NOT triggered
        productRepository.deleteById(created.getId());
        productRepository.flush();

        // Ensure database row is fully removed
        assertThat(productRepository.findById(created.getId())).isEmpty();

        // Act: Fetch product via service layer again; should hit Redis cache instead of DB
        ProductResponseDto fetchedFromCache = productService.getProductById(created.getId());

        // Assert: Ensure cached payload is returned intact despite database deletion
        assertThat(fetchedFromCache).isNotNull();
        assertThat(fetchedFromCache.getId()).isEqualTo(created.getId());
        assertThat(fetchedFromCache.getSku()).isEqualTo("PROD-KEY-01");
        assertThat(fetchedFromCache.getName()).isEqualTo("Mechanical Keyboard");
    }
}