package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Loads a product with a pessimistic write lock so version increments are serialized per row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /**
     * Check if a product already exists with the given SKU.
     * Useful for uniqueness validation during product creation and updates.
     *
     * @param sku the product SKU identifier to check
     * @return true if a product with the SKU exists, false otherwise
     */
    boolean existsBySku(String sku);
}