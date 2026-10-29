package dev.jahlopez.productcatalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for creating or updating a product record in the catalog.")
public class ProductRequestDto {

    @NotBlank(message = "SKU is required")
    @Schema(description = "Unique Stock Keeping Unit (SKU) identifier for the product", example = "PROD-HUB-001")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Schema(description = "Commercial name of the product", example = "USB-C Multiport Hub")
    private String name;

    @Schema(description = "Detailed description of product features and specifications", example = "Aluminum 7-in-1 adapter with 4K HDMI, Power Delivery, and USB 3.0 ports.")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @Schema(description = "Unit price of the product (must be strictly greater than zero)", example = "49.99")
    private BigDecimal price;

    @Schema(description = "Available stock inventory quantity", example = "150")
    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity must be zero or greater")
    private Integer stockQuantity;
}