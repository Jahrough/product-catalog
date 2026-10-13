package dev.jahlopez.productcatalog.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Represents the persistent Product domain entity mapped to the {@code products} table
 * in the relational database. Utilizes JPA annotations for ORM mapping and Lombok
 * annotations for boilerplate reduction and builder pattern instantiation.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /**
     * Unique primary key identifier for the product entity.
     * Generated automatically using database identity strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique Stock Keeping Unit (SKU) business identifier.
     * Must be non-null and strictly unique across the catalog.
     */
    @Column(nullable = false, unique = true)
    private String sku;

    /**
     * Commercial name of the product.
     * Must be non-null.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Detailed description of the product features and specifications.
     */
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
    private Integer stockQuantity;
}