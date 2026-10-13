package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and querying {@link Product} entity records.
 * Provides standard CRUD operations as well as custom finder methods for SKU lookups and validation.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its unique Stock Keeping Unit (SKU).
     *
     * @param sku the product SKU identifier
     * @return an Optional containing the found product, or empty if not found
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if a product already exists with the given SKU.
     * Useful for uniqueness validation during product creation and updates.
     *
     * @param sku the product SKU identifier to check
     * @return true if a product with the SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}