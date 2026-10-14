package dev.jahlopez.productcatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a product record returned by the catalog API.
 * Optimized for serialization/deserialization across REST boundaries and event streams.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object representing a product record returned by the catalog API.")
public class ProductResponseDto {

    @Schema(description = "Unique database primary key identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Unique Stock Keeping Unit (SKU) identifier", example = "PROD-HUB-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Schema(description = "Commercial name of the product", example = "USB-C Multiport Hub", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Detailed product description and specifications", example = "Aluminum 7-in-1 adapter with 4K HDMI, Power Delivery, and USB 3.0 ports.")
    private String description;

    @Schema(description = "Unit price of the product", example = "49.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "Current available inventory stock quantity", example = "150", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stockQuantity;

    @Schema(description = "Timestamp indicating when the product record was created", example = "2026-10-13T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp indicating when the product record was last updated", example = "2026-10-13T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}