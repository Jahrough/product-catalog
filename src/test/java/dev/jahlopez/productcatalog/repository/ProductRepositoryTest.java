package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data layer slice test class for {@link ProductRepository}.
 * Utilizes Spring Boot's {@link DataJpaTest} to validate JPA repository queries, entity mappings,
 * and database constraints in an isolated test environment.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Verifies that findBySku successfully retrieves a product record when a matching SKU exists in the database.
     */
    @Test
    void findBySku_WhenExists_ShouldReturnProduct() {
        // Arrange
        Product product = Product.builder()
                .sku("PROD-TEST-001")
                .name("Test Mechanical Keyboard")
                .description("RGB Wireless Keyboard")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(15)
                .build();
        entityManager.persistAndFlush(product);

        // Act
        Optional<Product> found = productRepository.findBySku("PROD-TEST-001");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("PROD-TEST-001");
        assertThat(found.get().getName()).isEqualTo("Test Mechanical Keyboard");
    }

    /**
     * Verifies that findBySku returns an empty Optional when the specified SKU does not exist.
     */
    @Test
    void findBySku_WhenNotFound_ShouldReturnEmpty() {
        // Act
        Optional<Product> found = productRepository.findBySku("NON-EXISTENT-SKU");

        // Assert
        assertThat(found).isEmpty();
    }

    /**
     * Verifies that saving a valid product entity successfully generates a database identifier and persists the record.
     */
    @Test
    void save_WithValidData_ShouldPersistProduct() {
        // Arrange
        Product product = Product.builder()
                .sku("PROD-TEST-002")
                .name("Test Wireless Mouse")
                .description("Ergonomic Optical Mouse")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(30)
                .build();

        // Act
        Product savedProduct = productRepository.save(product);

        // Assert
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getSku()).isEqualTo("PROD-TEST-002");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
    }
}