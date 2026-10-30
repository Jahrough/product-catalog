package dev.jahlopez.productcatalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.exception.ProductNotFoundException;
import dev.jahlopez.productcatalog.mapper.ProductMapper;
import dev.jahlopez.productcatalog.model.Product;
import dev.jahlopez.productcatalog.model.OutboxEvent;
import dev.jahlopez.productcatalog.repository.OutboxRepository;
import dev.jahlopez.productcatalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test suite for {@link ProductService}.
 * Utilizes Mockito to isolate business logic execution from underlying persistence, caching, and outbox event logging.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // Mock the data repository to isolate service logic from database operations
    @Mock
    private ProductRepository productRepository;

    // Mock the outbox repository to isolate transactional outbox event logging
    @Mock
    private OutboxRepository outboxRepository;

    // Mock the mapper to isolate entity-to-DTO conversion logic
    @Mock
    private ProductMapper productMapper;

    // Mock the ObjectMapper to isolate JSON serialization behavior during service tests
    @Mock
    private ObjectMapper objectMapper;

    // Inject the mocked dependencies into the ProductService instance under test
    @InjectMocks
    private ProductService productService;

    /**
     * Verifies that fetching all products returns a correctly mapped list of response DTOs.
     */
    @Test
    void getAllProducts_ShouldReturnListOfProducts() {
        // Arrange: Create sample product entity and expected response DTO
        Product product = Product.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .description("Multiport adapter")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .version(1)
                .build();

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .description("Multiport adapter")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .build();

        // Stub repository and mapper behaviors
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // Act: Call service method
        List<ProductResponseDto> products = productService.getAllProducts();

        // Assert: Validate the returned list size and mapped fields
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("USB-C Hub");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));

        // Verify mock interactions
        verify(productRepository, times(1)).findAll();
        verify(productMapper, times(1)).toResponseDto(product);
    }

    /**
     * Verifies that retrieving a product by ID when it exists returns the corresponding response DTO.
     */
    @Test
    void getProductById_WhenProductExists_ShouldReturnProduct() {
        // Arrange: Prepare sample product and response DTO
        Product product = Product.builder()
                .id(1L)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .version(1)
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

        // Act: Fetch product by ID
        ProductResponseDto found = productService.getProductById(1L);

        // Assert: Ensure the returned DTO matches expectations
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(product.getId());
        assertThat(found.getName()).isEqualTo("USB-C Hub");

        // Verify mock interactions
        verify(productRepository, times(1)).findById(1L);
        verify(productMapper, times(1)).toResponseDto(product);
    }

    /**
     * Verifies that attempting to retrieve a non-existent product by ID throws a {@link ProductNotFoundException}.
     */
    @Test
    void getProductById_WhenProductNotFound_ShouldThrowException() {
        // Arrange: Stub repository to return empty Optional for an unknown ID
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert: Verify that calling the method throws ProductNotFoundException
        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById(99L);
        });

        // Verify repository was queried and mapper was never invoked due to early exception
        verify(productRepository, times(1)).findById(99L);
        verifyNoInteractions(productMapper);
    }

    /**
     * Verifies that creating a new product maps the request DTO to an entity, saves it via the repository,
     * logs an outbox event intent, and returns the populated response DTO.
     */
    @Test
    void createProduct_ShouldSaveAndReturnProduct() throws Exception {
        // Arrange: Set up incoming request DTO and expected intermediate/output objects
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
                .version(1)
                .build();

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        // Stub mapper, repository, object mapper, and outbox repository behaviors
        when(productMapper.toEntity(any(ProductRequestDto.class))).thenReturn(productToEntity);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toResponseDto(savedProduct)).thenReturn(responseDto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"123-abc\"}");
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // Act: Execute create product operation
        ProductResponseDto result = productService.createProduct(requestDto);

        // Assert: Validate saved response fields
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Wireless Charger");
        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(29.99));

        // Verify all expected collaborators were called correctly
        verify(productMapper, times(1)).toEntity(any(ProductRequestDto.class));
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productMapper, times(1)).toResponseDto(savedProduct);
        verify(objectMapper, times(1)).writeValueAsString(any());
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void createProduct_whenEventSerializationFails_shouldNotSaveOutboxEvent() throws Exception {
        ProductRequestDto request = new ProductRequestDto();
        Product product = Product.builder()
                .id(1L)
                .sku("PROD-WIR-001")
                .name("Wireless Charger")
                .price(new BigDecimal("29.99"))
                .stockQuantity(40)
                .version(1)
                .build();

        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failure") {});

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to serialize outbox event");

        verifyNoInteractions(outboxRepository);
    }

    /**
     * Verifies that deleting an existing product successfully removes the entity from the database,
     * logs a deletion event intent to the outbox table, and executes all expected repository interactions.
     */
    @Test
    void deleteProduct_WhenProductExists_ShouldDeleteAndSaveOutboxEvent() throws Exception {
        // Arrange: Prepare a sample product entity simulating an existing record in the database
        Long productId = 1L;
        Product product = Product.builder()
                .id(productId)
                .sku("PROD-HUB-001")
                .name("USB-C Hub")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .version(1)
                .build();

        // Stub repository, object mapper, and outbox repository behaviors
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"456-def\"}");
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // Act: Invoke the service method responsible for product deletion and outbox logging
        productService.deleteProduct(productId);

        // Assert: Verify that the product was found, explicitly deleted, and an outbox event was logged
        verify(productRepository, times(1)).findByIdForUpdate(productId);
        verify(productRepository, times(1)).delete(product);
        verify(objectMapper, times(1)).writeValueAsString(any());
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }
}