package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its unique Stock Keeping Unit (SKU).
     *
     * @param sku the product SKU
     * @return an Optional containing the product if found
     */
    Optional<Product> findBySku(String sku);

    /**
     * Check if a product already exists with the given SKU.
     * Useful for validation during product creation.
     *
     * @param sku the product SKU
     * @return true if a product with the SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}