package dev.jahlopez.productcatalog.controller;

import dev.jahlopez.productcatalog.config.swagger.ApiReadResponses;
import dev.jahlopez.productcatalog.config.swagger.ApiWriteResponses;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing production-grade endpoints for managing the product catalog.
 * Implements clean OpenAPI 3 documentation via custom meta-annotations, request validation,
 * and standardized error responses.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(
        name = "Product Catalog API",
        description = "High-performance microservice endpoints for managing product catalog data, pricing, inventory, and SKU lookups."
)
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "Create a new product",
            description = "Validates and persists a new product record into the database. Evicts product cache entries to maintain consistency."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product successfully created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))
    )
    @ApiWriteResponses
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        ProductResponseDto createdProduct = productService.createProduct(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(
            summary = "Retrieve a product by internal ID",
            description = "Fetches product details by database primary key. Leverages Redis caching for high-traffic read performance."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product successfully retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))
    )
    @ApiReadResponses
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(
            summary = "Retrieve a product by Stock Keeping Unit (SKU)",
            description = "Fetches product details using its unique business SKU identifier. Cached in Redis for low-latency lookups."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product successfully retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))
    )
    @ApiReadResponses
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @Operation(
            summary = "List all products",
            description = "Retrieves a complete list of all active products in the catalog. Results are cached under the 'productsList' cache tier."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved product list",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class)))
    )
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(
            summary = "Delete product by ID",
            description = "Removes a product record from the database by its primary key and evicts all associated product and list caches."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Product deleted successfully (No Content)",
            content = @Content
    )
    @ApiReadResponses
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}