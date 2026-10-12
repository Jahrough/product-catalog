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

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    @CacheEvict(value = {"products", "productsList"}, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = productMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponseDto(savedProduct);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return productMapper.toResponseDto(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#sku")
    public ProductResponseDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toResponseDto(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productsList")
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"products", "productsList"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

}