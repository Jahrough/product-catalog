package dev.jahlopez.productcatalog.mapper;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProductMapper}.
 *
 * <p>Verifies mapping behavior between product request DTOs, domain entities,
 * and response DTOs. Tests exercise the MapStruct-generated implementation
 * directly, without initializing a Spring application context.</p>
 *
 * <p>These tests cover business-field mapping, persistence-managed field
 * boundaries, and propagation of auditing timestamps into API responses.</p>
 */
class ProductMapperTest {

    private static final String TEST_SKU = "SKU-MAP-01";
    private static final String TEST_NAME = "Ergonomic Chair";
    private static final String TEST_DESCRIPTION = "Adjustable mesh office chair";
    private static final BigDecimal TEST_PRICE = new BigDecimal("249.99");
    private static final int TEST_STOCK_QUANTITY = 25;
    private static final Long TEST_PRODUCT_ID = 42L;

    private static final LocalDateTime TEST_TIMESTAMP =
            LocalDateTime.of(2026, 10, 15, 10, 30, 0);

    private ProductMapper productMapper;

    /**
     * Creates the generated mapper implementation for each test.
     *
     * <p>Direct instantiation is appropriate because the mapper is used as a
     * stateless component without Spring-managed dependencies.</p>
     */
    @BeforeEach
    void setUp() {
        productMapper = new ProductMapperImpl();
    }

    /**
     * Verifies that client-supplied product fields are mapped to an entity.
     *
     * <p>The request must not assign persistence-managed values such as the
     * generated database identifier or auditing timestamps.</p>
     */
    @Test
    void shouldMapProductRequestDtoToProductEntity() {
        // Arrange
        ProductRequestDto requestDto = createProductRequestDto();

        // Act
        Product product = productMapper.toEntity(requestDto);

        // Assert: the result exists and all business fields are preserved.
        assertThat(product)
                .isNotNull()
                .satisfies(mappedProduct -> {
                    assertThat(mappedProduct.getSku()).isEqualTo(TEST_SKU);
                    assertThat(mappedProduct.getName()).isEqualTo(TEST_NAME);
                    assertThat(mappedProduct.getDescription()).isEqualTo(TEST_DESCRIPTION);
                    assertThat(mappedProduct.getPrice()).isEqualByComparingTo(TEST_PRICE);
                    assertThat(mappedProduct.getStockQuantity()).isEqualTo(TEST_STOCK_QUANTITY);
                });

        // The identifier is generated during persistence, not supplied by the request.
        assertThat(product.getId()).isNull();

        // Auditing timestamps are assigned by the persistence lifecycle.
        assertThat(product.getCreatedAt()).isNull();
        assertThat(product.getUpdatedAt()).isNull();
    }

    /**
     * Verifies that an entity's business data and auditing metadata are
     * transferred to the response DTO.
     */
    @Test
    void shouldMapProductEntityToProductResponseDto() {
        // Arrange
        Product product = createProductEntity();

        // Act
        ProductResponseDto responseDto = productMapper.toResponseDto(product);

        // Assert
        assertThat(responseDto)
                .isNotNull()
                .satisfies(dto -> {
                    assertThat(dto.getId()).isEqualTo(TEST_PRODUCT_ID);
                    assertThat(dto.getSku()).isEqualTo(TEST_SKU);
                    assertThat(dto.getName()).isEqualTo(TEST_NAME);
                    assertThat(dto.getDescription()).isEqualTo(TEST_DESCRIPTION);
                    assertThat(dto.getPrice()).isEqualByComparingTo(TEST_PRICE);
                    assertThat(dto.getStockQuantity()).isEqualTo(TEST_STOCK_QUANTITY);
                    assertThat(dto.getCreatedAt()).isEqualTo(TEST_TIMESTAMP);
                    assertThat(dto.getUpdatedAt()).isEqualTo(TEST_TIMESTAMP);
                });
    }

    /**
     * Builds a request DTO containing representative client-supplied data.
     *
     * @return a populated product request DTO
     */
    private ProductRequestDto createProductRequestDto() {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku(TEST_SKU);
        requestDto.setName(TEST_NAME);
        requestDto.setDescription(TEST_DESCRIPTION);
        requestDto.setPrice(TEST_PRICE);
        requestDto.setStockQuantity(TEST_STOCK_QUANTITY);

        return requestDto;
    }

    /**
     * Builds a product entity with a fixed identifier and timestamps.
     *
     * <p>Fixed values make the response mapping assertions deterministic and
     * independent of the system clock.</p>
     *
     * @return a populated product entity
     */
    private Product createProductEntity() {
        return Product.builder()
                .id(TEST_PRODUCT_ID)
                .sku(TEST_SKU)
                .name(TEST_NAME)
                .description(TEST_DESCRIPTION)
                .price(TEST_PRICE)
                .stockQuantity(TEST_STOCK_QUANTITY)
                .createdAt(TEST_TIMESTAMP)
                .updatedAt(TEST_TIMESTAMP)
                .build();
    }
}