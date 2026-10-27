package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.config.JpaAuditingConfig;
import dev.jahlopez.productcatalog.model.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-layer tests for {@link ProductRepository}.
 *
 * <p>Validates product persistence and SKU-based lookup behavior against a real
 * PostgreSQL database managed by Testcontainers. This helps verify entity
 * mappings, repository queries, and PostgreSQL-specific behavior consistently
 * across local development and CI environments.</p>
 *
 * <p>{@link DataJpaTest} configures a JPA-focused Spring test context.
 * The auditing configuration is imported explicitly because a JPA slice test
 * does not necessarily load the full application configuration.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductRepositoryTest {

    private static final String EXISTING_SKU = "PROD-TEST-001";
    private static final String MISSING_SKU = "NON-EXISTENT-SKU";
    private static final String PRODUCT_NAME = "Test Mechanical Keyboard";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("99.99");

    /**
     * PostgreSQL instance shared by the tests in this class.
     *
     * <p>Testcontainers manages the container lifecycle, isolating these tests
     * from any PostgreSQL instance running directly on the developer's machine.</p>
     */
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine")
                    .withDatabaseName("product_catalog_test")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Registers the PostgreSQL container's connection settings with Spring.
     *
     * <p>The JDBC URL includes the dynamically mapped host port, so the tests
     * do not depend on a fixed local PostgreSQL port.</p>
     *
     * @param registry registry for dynamically supplied Spring properties
     */
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Explicitly configure Flyway to use the same PostgreSQL container.
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.flyway.driver-class-name", () -> "org.postgresql.Driver");
    }

    /** JPA test utility for persisting entities and flushing changes. */
    @Autowired
    private EntityManager entityManager;

    /** Repository being tested. */
    @Autowired
    private ProductRepository productRepository;

    /**
     * Verifies that a product can be retrieved by SKU when a matching record
     * exists in the database.
     */
    @Test
    void findBySku_whenProductExists_returnsProduct() {
        // Arrange
        Product product = Product.builder()
                .sku(EXISTING_SKU)
                .name(PRODUCT_NAME)
                .description("RGB Wireless Keyboard")
                .price(PRODUCT_PRICE)
                .stockQuantity(15)
                .build();

        entityManager.persist(product);
        entityManager.flush();
        entityManager.clear();

        // Act
        var result = productRepository.findBySku(EXISTING_SKU);

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getSku()).isEqualTo(EXISTING_SKU);
                    assertThat(found.getName()).isEqualTo(PRODUCT_NAME);
                    assertThat(found.getPrice()).isEqualByComparingTo(PRODUCT_PRICE);
                });
    }

    /**
     * Verifies that searching for a nonexistent SKU returns an empty Optional.
     */
    @Test
    void findBySku_whenProductDoesNotExist_returnsEmptyOptional() {
        // Act
        var result = productRepository.findBySku(MISSING_SKU);

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * Verifies that saving a valid product assigns an identifier and persists
     * the expected field values.
     */
    @Test
    void save_withValidProduct_persistsProduct() {
        // Arrange
        Product product = Product.builder()
                .sku("PROD-TEST-002")
                .name("Test Wireless Mouse")
                .description("Ergonomic Optical Mouse")
                .price(new BigDecimal("49.99"))
                .stockQuantity(30)
                .build();

        // Act: flush forces pending SQL operations to execute.
        Product savedProduct = productRepository.saveAndFlush(product);

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getSku()).isEqualTo("PROD-TEST-002");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo("49.99");
    }
}