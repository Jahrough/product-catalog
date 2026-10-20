package dev.jahlopez.productcatalog.validation;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enterprise validation test suite for verifying Bean Validation (JSR-380 / Jakarta Validation)
 * constraints on {@link ProductRequestDto}.
 *
 * <p>Ensures that invalid DTO payloads are rejected before hitting domain business logic
 * or persistence boundaries.
 */
@DisplayName("ProductRequestDto Bean Validation Tests")
class ProductDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("Valid Payload Tests")
    class ValidPayloadTests {

        @Test
        @DisplayName("Should pass validation when all required fields are valid")
        void shouldPassWhenDtoIsValid() {
            ProductRequestDto dto = new ProductRequestDto();
            dto.setSku("PROD-HUB-001");
            dto.setName("USB-C Multiport Hub");
            dto.setDescription("Aluminum 7-in-1 adapter with 4K HDMI, Power Delivery, and USB 3.0 ports.");
            dto.setPrice(new BigDecimal("49.99"));
            dto.setStockQuantity(150);

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass validation when optional description and stockQuantity are null")
        void shouldPassWhenOptionalFieldsAreNull() {
            ProductRequestDto dto = new ProductRequestDto();
            dto.setSku("PROD-HUB-002");
            dto.setName("Wireless Ergonomic Mouse");
            dto.setPrice(new BigDecimal("29.99"));
            dto.setDescription(null);
            dto.setStockQuantity(null);

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("SKU Constraint Tests")
    class SkuConstraintTests {

        @Test
        @DisplayName("Should fail validation when SKU is null")
        void shouldFailWhenSkuIsNull() {
            ProductRequestDto dto = createValidDto();
            dto.setSku(null);

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("sku")
                            && v.getMessage().equals("SKU is required"));
        }

        @Test
        @DisplayName("Should fail validation when SKU is empty or whitespace")
        void shouldFailWhenSkuIsBlank() {
            ProductRequestDto dto = createValidDto();
            dto.setSku("   ");

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("sku")
                            && v.getMessage().equals("SKU is required"));
        }
    }

    @Nested
    @DisplayName("Name Constraint Tests")
    class NameConstraintTests {

        @Test
        @DisplayName("Should fail validation when product name is null")
        void shouldFailWhenNameIsNull() {
            ProductRequestDto dto = createValidDto();
            dto.setName(null);

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("name")
                            && v.getMessage().equals("Product name is required"));
        }

        @Test
        @DisplayName("Should fail validation when product name is blank")
        void shouldFailWhenNameIsBlank() {
            ProductRequestDto dto = createValidDto();
            dto.setName("");

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("name")
                            && v.getMessage().equals("Product name is required"));
        }
    }

    @Nested
    @DisplayName("Price Constraint Tests")
    class PriceConstraintTests {

        @Test
        @DisplayName("Should fail validation when price is null")
        void shouldFailWhenPriceIsNull() {
            ProductRequestDto dto = createValidDto();
            dto.setPrice(null);

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("price")
                            && v.getMessage().equals("Price is required"));
        }

        @Test
        @DisplayName("Should fail validation when price is exactly zero")
        void shouldFailWhenPriceIsZero() {
            ProductRequestDto dto = createValidDto();
            dto.setPrice(new BigDecimal("0.00"));

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("price")
                            && v.getMessage().equals("Price must be greater than zero"));
        }

        @Test
        @DisplayName("Should fail validation when price is negative")
        void shouldFailWhenPriceIsNegative() {
            ProductRequestDto dto = createValidDto();
            dto.setPrice(new BigDecimal("-10.00"));

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .hasSize(1)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("price")
                            && v.getMessage().equals("Price must be greater than zero"));
        }

        @Test
        @DisplayName("Should pass validation when price is slightly above zero")
        void shouldPassWhenPriceIsGreaterThanZero() {
            ProductRequestDto dto = createValidDto();
            dto.setPrice(new BigDecimal("0.01"));

            Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }
    }

    /**
     * Helper factory method to build a fully valid {@link ProductRequestDto} baseline.
     */
    private ProductRequestDto createValidDto() {
        ProductRequestDto dto = new ProductRequestDto();
        dto.setSku("PROD-HUB-001");
        dto.setName("USB-C Multiport Hub");
        dto.setDescription("Aluminum 7-in-1 adapter.");
        dto.setPrice(new BigDecimal("49.99"));
        dto.setStockQuantity(100);
        return dto;
    }
}