package dev.jahlopez.productcatalog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.event.ProductEvent;
import dev.jahlopez.productcatalog.exception.ProductNotFoundException;
import dev.jahlopez.productcatalog.mapper.ProductMapper;
import dev.jahlopez.productcatalog.model.OutboxEvent;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.repository.OutboxRepository;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service component responsible for orchestrating product catalog business logic,
 * database persistence transactions, type-safe object mapping, Redis caching, and transactional outbox event logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates and persists a new product record in the database.
     * Transactionally logs a CREATED event intent to the outbox table and evicts all cache tiers to maintain consistency.
     *
     * @param requestDto the product creation payload containing SKU, name, price, etc.
     * @return the saved product response DTO
     */
    @Transactional
    @CacheEvict(value = {"productsById", "productsBySku", "productsList"}, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = productMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);

        // Construct the domain event with version and metadata
        ProductEvent event = ProductEvent.builder()
                .schemaVersion(1)
                .eventId(UUID.randomUUID())
                .version(savedProduct.getVersion())
                .productId(savedProduct.getId())
                .sku(savedProduct.getSku())
                .name(savedProduct.getName())
                .price(savedProduct.getPrice())
                .stockQuantity(savedProduct.getStockQuantity())
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        // Transactionally save event intent to the outbox table
        saveOutboxEvent(savedProduct.getId().toString(), "ProductCreated", event);

        return productMapper.toResponseDto(savedProduct);
    }

    /**
     * Retrieves product details by its unique primary key ID.
     * Leverages Redis caching ('productsById' cache tier) for high-performance read throughput.
     *
     * @param id the database primary key identifier
     * @return the product response DTO
     * @throws ProductNotFoundException if no product exists with the given ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsById", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return productMapper.toResponseDto(product);
    }

    /**
     * Retrieves product details by its unique Stock Keeping Unit (SKU).
     * Leverages Redis caching ('productsBySku' cache tier) for low-latency lookups.
     *
     * @param sku the unique product SKU identifier
     * @return the product response DTO
     * @throws ProductNotFoundException if no product exists with the given SKU
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "productsBySku", key = "#sku")
    public ProductResponseDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku));
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
     * Transactionally logs a DELETED event intent to the outbox table and evicts all cache tiers to ensure cache consistency.
     *
     * @param id the database primary key identifier of the product to delete
     * @throws ProductNotFoundException if no product exists with the given ID
     */
    @Transactional
    @CacheEvict(value = {"productsById", "productsBySku", "productsList"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        // Construct the domain event with version metadata before deletion
        ProductEvent event = ProductEvent.builder()
                .schemaVersion(1)
                .eventId(UUID.randomUUID())
                .version(product.getVersion())
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .eventType(ProductEvent.EventType.DELETED)
                .timestamp(LocalDateTime.now())
                .build();

        // Transactionally save event intent to the outbox table
        saveOutboxEvent(product.getId().toString(), "ProductDeleted", event);

        productRepository.delete(product);
    }

    /**
     * Helper method to serialize a domain event and persist it into the outbox table.
     */
    private void saveOutboxEvent(String aggregateId, String eventType, ProductEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.eventId())
                    .aggregateType("product")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .schemaVersion(event.schemaVersion())
                    .processed(false)
                    .build();

            outboxRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Failed to serialize and save outbox event for aggregateId: {}", aggregateId, e);
            throw new RuntimeException("Failed to persist outbox event", e);
        }
    }
}