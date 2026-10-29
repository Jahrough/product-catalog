package dev.jahlopez.productcatalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the persistent Product domain entity mapped to the {@code products} table
 * in the relational database. Utilizes JPA annotations for ORM mapping, auditing listeners,
 * optimistic locking versioning, and explicit equals/hashCode boundaries for high-performance entity management.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_sku", columnList = "sku", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class Product {

    /**
     * Unique primary key identifier for the product entity.
     * Generated automatically using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Unique Stock Keeping Unit (SKU) business identifier.
     * Must be non-null and strictly unique across the catalog.
     */
    @Column(nullable = false, unique = true, length = 64)
    @EqualsAndHashCode.Include
    private String sku;

    /**
     * Commercial name of the product.
     * Must be non-null.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Detailed description of the product features and specifications.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Unit price of the product.
     * Mapped with precision 10 and scale 2 to ensure accurate monetary calculations.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Available inventory stock quantity for fulfillment.
     */
    @Column(nullable = false)
    private Integer stockQuantity;

    /**
     * Monotonic aggregate version. Starts at {@link #INITIAL_VERSION} on creation and is incremented
     * by exactly one on every update and delete, providing ordering metadata for downstream stale-event guards.
     * Concurrent writers are serialized with a pessimistic lock in the service layer.
     */
    @Column(nullable = false)
    private Integer version;

    public static final int INITIAL_VERSION = 1;

    @PrePersist
    void initVersion() {
        if (this.version == null) {
            this.version = INITIAL_VERSION;
        }
    }

    /** Advances the aggregate version by one and returns the new value. */
    public int incrementVersion() {
        this.version = Math.incrementExact(this.version == null ? INITIAL_VERSION : this.version);
        return this.version;
    }

    /** Returns the next event version without changing the deleted aggregate. */
    public long nextVersion() {
        return (long) (this.version == null ? INITIAL_VERSION : this.version) + 1;
    }

    /**
     * Timestamp indicating when the product entity was initially created.
     * Automatically populated via Spring Data JPA auditing.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the product entity was last modified.
     * Automatically updated via Spring Data JPA auditing.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}