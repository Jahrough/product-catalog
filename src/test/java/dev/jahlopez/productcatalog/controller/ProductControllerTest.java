package dev.jahlopez.productcatalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllProducts_ShouldReturnListOfProducts() throws Exception {
        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-KEY-001")
                .name("Mechanical Keyboard")
                .description("RGB Wireless Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].sku").value("PROD-KEY-001"))
                .andExpect(jsonPath("$[0].name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$[0].price").value(149.99));
    }

    @Test
    void getProductById_WhenFound_ShouldReturnProduct() throws Exception {
        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-KEY-001")
                .name("Mechanical Keyboard")
                .description("RGB Wireless Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .build();

        when(productService.getProductById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("PROD-KEY-001"))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"));
    }

    @Test
    void getProductById_WhenNotFound_ShouldReturn404() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new EntityNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createProduct_WithValidData_ShouldReturnCreated() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-MOUSE-001");
        requestDto.setName("Ergonomic Mouse");
        requestDto.setDescription("Wireless mouse");
        requestDto.setPrice(BigDecimal.valueOf(79.99));
        requestDto.setStockQuantity(30);

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .id(2L)
                .sku(requestDto.getSku())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .stockQuantity(requestDto.getStockQuantity())
                .build();

        when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.sku").value("PROD-MOUSE-001"))
                .andExpect(jsonPath("$.name").value("Ergonomic Mouse"))
                .andExpect(jsonPath("$.price").value(79.99));
    }

    @Test
    void createProduct_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        ProductRequestDto requestDto = new ProductRequestDto();
        requestDto.setSku(""); // Blank SKU violates @NotBlank
        requestDto.setName(""); // Blank name violates @NotBlank
        requestDto.setPrice(BigDecimal.valueOf(-5.0)); // Negative price violates validation constraints

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}