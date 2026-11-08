package dev.jahlopez.productcatalog.event;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable product lifecycle payload stored in the Catalog outbox alongside a product mutation.
 * {@code eventId} identifies this event, while {@code productId} and {@code version} identify
 * the product state and its ordering metadata. Persisting this record does not itself publish it
 * to Kafka or guarantee downstream delivery; the outbox row and CDC connector provide that path.
 *
 * The Catalog currently builds the payload from the product's complete state, rather than a
 * partial change set.
 */
@Builder
public record ProductEvent(
        Integer schemaVersion,
        UUID eventId,
        Long version,
        Long productId,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        EventType eventType,
        LocalDateTime timestamp
) {
    public ProductEvent {
        if (version == null || version < 1) {
            throw new IllegalArgumentException("Event version must be a positive integer");
        }
    }

    /**
     * Defines the type of lifecycle action that occurred for the product.
     */
    public enum EventType {
        /** Indicates a new product has been created and added to the catalog. */
        CREATED,

        /** Indicates an existing product's details or pricing have been updated. */
        UPDATED,

        /** Indicates a product has been deleted or removed from the catalog. */
        DELETED
    }
}