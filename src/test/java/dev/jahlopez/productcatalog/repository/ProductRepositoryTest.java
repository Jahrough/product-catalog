package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.config.JpaAuditingConfig;
import dev.jahlopez.productcatalog.model.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

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
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class ProductRepositoryTest extends PostgresRepositoryTestSupport {

    private static final String EXISTING_SKU = "PROD-TEST-001";
    private static final String MISSING_SKU = "NON-EXISTENT-SKU";
    private static final String PRODUCT_NAME = "Test Mechanical Keyboard";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("99.99");

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