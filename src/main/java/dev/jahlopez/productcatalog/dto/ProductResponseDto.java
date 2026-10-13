package dev.jahlopez.productcatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object representing a product record returned by the catalog API.")
public class ProductResponseDto {

    @Schema(description = "Unique database primary key identifier", example = "1")
    private Long id;

    @Schema(description = "Unique Stock Keeping Unit (SKU) identifier", example = "PROD-HUB-001")
    private String sku;

    @Schema(description = "Commercial name of the product", example = "USB-C Multiport Hub")
    private String name;

    @Schema(description = "Detailed product description and specifications", example = "Aluminum 7-in-1 adapter with 4K HDMI, Power Delivery, and USB 3.0 ports.")
    private String description;

    @Schema(description = "Unit price of the product", example = "49.99")
    private BigDecimal price;

    @Schema(description = "Current available inventory stock quantity", example = "150")
    private Integer stockQuantity;
}