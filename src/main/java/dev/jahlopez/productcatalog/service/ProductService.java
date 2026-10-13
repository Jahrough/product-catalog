package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.mapper.ProductMapper;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service component responsible for orchestrating product catalog business logic,
 * database persistence transactions, type-safe object mapping, and Redis caching integration.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * Creates and persists a new product record in the database.
     * Evicts all entries in the 'products' and 'productsList' caches to maintain data consistency.
     *
     * @param requestDto the product creation payload containing SKU, name, price, etc.
     * @return the saved product response DTO
     */
    @Transactional
    @CacheEvict(value = {"products", "productsList"}, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = productMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponseDto(savedProduct);
    }

    /**
     * Retrieves product details by its unique primary key ID.
     * Leverages Redis caching ('products' cache tier) for high-performance read throughput.
     *
     * @param id the database primary key identifier
     * @return the product response DTO
     * @throws EntityNotFoundException if no product exists with the given ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return productMapper.toResponseDto(product);
    }

    /**
     * Retrieves product details by its unique Stock Keeping Unit (SKU).
     * Leverages Redis caching ('products' cache tier) for low-latency lookups.
     *
     * @param sku the unique product SKU identifier
     * @return the product response DTO
     * @throws EntityNotFoundException if no product exists with the given SKU
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
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
                .collect(Collectors.toList());
    }

    /**
     * Deletes a product record by its database primary key ID.
     * Evicts all entries in the 'products' and 'productsList' caches to ensure cache consistency.
     *
     * @param id the database primary key identifier of the product to delete
     * @throws EntityNotFoundException if no product exists with the given ID
     */
    @Transactional
    @CacheEvict(value = {"products", "productsList"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}