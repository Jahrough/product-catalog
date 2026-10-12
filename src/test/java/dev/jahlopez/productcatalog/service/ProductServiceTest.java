package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.mapper.ProductMapper;
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

    @Mock
    private ProductMapper productMapper;

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

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .description("Multiport adapter")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // Act
        List<ProductResponseDto> products = productService.getAllProducts();

        // Assert
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("USB-C Hub");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        verify(productRepository, times(1)).findAll();
        verify(productMapper, times(1)).toResponseDto(product);
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

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // Act
        ProductResponseDto found = productService.getProductById(1L);

        // Assert
        assertNotNull(found);
        assertEquals(product.getId(), found.getId());
        assertThat(found.getName()).isEqualTo("USB-C Hub");
        verify(productRepository, times(1)).findById(1L);
        verify(productMapper, times(1)).toResponseDto(product);
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
        verifyNoInteractions(productMapper);
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

        Product productToEntity = Product.builder()
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        Product savedProduct = Product.builder()
                .id(1L)
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        when(productMapper.toEntity(any(ProductRequestDto.class))).thenReturn(productToEntity);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponseDto(savedProduct)).thenReturn(responseDto);

        // Act
        ProductResponseDto result = productService.createProduct(requestDto);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Wireless Charger");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        verify(productMapper, times(1)).toEntity(any(ProductRequestDto.class));
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productMapper, times(1)).toResponseDto(savedProduct);
    }
}