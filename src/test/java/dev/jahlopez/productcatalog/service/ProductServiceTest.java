package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_ShouldReturnListOfProducts() {
        // Arrange
        Product product = Product.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .description("Multiport adapter")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product));

        // Act
        List<ProductResponseDto> products = productService.getAllProducts();

        // Assert
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("USB-C Hub");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProduct() {
        // Arrange
        Product product = Product.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        ProductResponseDto found = productService.getProductById(1L);

        // Assert
        assertNotNull(found);
        assertEquals(product.getId(), found.getId());
        assertThat(found.getName()).isEqualTo("USB-C Hub");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_WhenProductNotFound_ShouldThrowException() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            productService.getProductById(99L);
        });
        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    void createProduct_ShouldSaveAndReturnProduct() {
        // Arrange
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-WIR-001");
        requestDto.setName("Wireless Charger");
        requestDto.setDescription("Fast charging pad");
        requestDto.setPrice(BigDecimal.valueOf(29.99));
        requestDto.setStockQuantity(40);

        Product savedProduct = Product.builder()
                .id(1L)
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductResponseDto result = productService.createProduct(requestDto);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Wireless Charger");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        verify(productRepository, times(1)).save(any(Product.class));
    }
}