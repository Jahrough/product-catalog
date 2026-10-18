package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.event.ProductEvent;
import dev.jahlopez.productcatalog.mapper.ProductMapper;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service component responsible for orchestrating product catalog business logic,
 * database persistence transactions, type-safe object mapping, Redis caching, and Kafka event broadcasting.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductEventPublisher productEventPublisher;

    /**
     * Creates and persists a new product record in the database.
     * Broadcasts a CREATED event to Kafka and evicts all cache tiers to maintain consistency.
     *
     * @param requestDto the product creation payload containing SKU, name, price, etc.
     * @return the saved product response DTO
     */
    @Transactional
    @CacheEvict(value = {"productsById", "productsBySku", "productsList"}, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = productMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);

        // Publish CREATED event to Kafka
        productEventPublisher.publishProductEvent(ProductEvent.builder()
                .productId(savedProduct.getId())
                .sku(savedProduct.getSku())
                .name(savedProduct.getName())
                .price(savedProduct.getPrice())
                .stockQuantity(savedProduct.getStockQuantity())
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build());

        return productMapper.toResponseDto(savedProduct);
    }

    /**
     * Retrieves product details by its unique primary key ID.
     * Leverages Redis caching ('productsById' cache tier) for high-performance read throughput.
     *
     * @param id the database primary key identifier
     * @return the product response DTO
     * @throws EntityNotFoundException if no product exists with the given ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsById", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return productMapper.toResponseDto(product);
    }

    /**
     * Retrieves product details by its unique Stock Keeping Unit (SKU).
     * Leverages Redis caching ('productsBySku' cache tier) for low-latency lookups.
     *
     * @param sku the unique product SKU identifier
     * @return the product response DTO
     * @throws EntityNotFoundException if no product exists with the given SKU
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsBySku", key = "#sku")
    public ProductResponseDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toResponseDto(product);
    }

    /**
     * Retrieves a complete list of all active products in the catalog.
     * Results are cached under the 'productsList' cache tier.
     *
     * @return a list of all product response DTOs
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsList")
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponseDto)
                .toList();
    }

    /**
     * Deletes a product record by its database primary key ID.
     * Broadcasts a DELETED event to Kafka and evicts all cache tiers to ensure cache consistency.
     *
     * @param id the database primary key identifier of the product to delete
     * @throws EntityNotFoundException if no product exists with the given ID
     */
    @Transactional
    @CacheEvict(value = {"productsById", "productsBySku", "productsList"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        productRepository.delete(product);

        // Publish DELETED event to Kafka
        productEventPublisher.publishProductEvent(ProductEvent.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .eventType(ProductEvent.EventType.DELETED)
                .timestamp(LocalDateTime.now())
                .build());
    }
}