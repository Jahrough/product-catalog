package dev.jahlopez.productcatalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.exception.ProductNotFoundException;
import dev.jahlopez.productcatalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final long PRODUCT_ID = 1L;
    private static final long MISSING_PRODUCT_ID = 99L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void getAllProducts_shouldReturnProducts() throws Exception {
        when(productService.getAllProducts())
                .thenReturn(List.of(productResponse()));

        mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(PRODUCT_ID))
                .andExpect(jsonPath("$[0].sku").value("PROD-KEY-001"))
                .andExpect(jsonPath("$[0].name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$[0].price").value(149.99));
    }

    @Test
    void getProductById_whenFound_shouldReturnProduct() throws Exception {
        when(productService.getProductById(PRODUCT_ID))
                .thenReturn(productResponse());

        mockMvc.perform(get(PRODUCTS_URL + "/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.sku").value("PROD-KEY-001"))
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.price").value(149.99));
    }

    @Test
    void getProductById_whenMissing_shouldReturnNotFound() throws Exception {
        when(productService.getProductById(MISSING_PRODUCT_ID))
                .thenThrow(new ProductNotFoundException(
                        "Product not found with id: " + MISSING_PRODUCT_ID));

        mockMvc.perform(get(PRODUCTS_URL + "/{id}", MISSING_PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createProduct_withValidData_shouldReturnCreated() throws Exception {
        ProductRequestDto request = validProductRequest();

        ProductResponseDto response = ProductResponseDto.builder()
                .id(2L)
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .build();

        when(productService.createProduct(any(ProductRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.sku").value(request.getSku()))
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.price").value(79.99));

        verify(productService).createProduct(any(ProductRequestDto.class));
    }

    @Test
    void createProduct_withInvalidData_shouldReturnBadRequest() throws Exception {
        ProductRequestDto request = new ProductRequestDto();
        request.setSku("");
        request.setName("");
        request.setPrice(new BigDecimal("-5.00"));

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_PAYLOAD"));

        verifyNoInteractions(productService);
    }

    @Test
    void deleteProduct_whenFound_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete(PRODUCTS_URL + "/{id}", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(PRODUCT_ID);
    }

    @Test
    void deleteProduct_whenMissing_shouldReturnNotFound() throws Exception {
        doThrow(new ProductNotFoundException(
                "Product not found with id: " + MISSING_PRODUCT_ID))
                .when(productService)
                .deleteProduct(MISSING_PRODUCT_ID);

        mockMvc.perform(delete(
                        PRODUCTS_URL + "/{id}", MISSING_PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    private static ProductResponseDto productResponse() {
        return ProductResponseDto.builder()
                .id(PRODUCT_ID)
                .sku("PROD-KEY-001")
                .name("Mechanical Keyboard")
                .description("RGB Wireless Keyboard")
                .price(new BigDecimal("149.99"))
                .stockQuantity(50)
                .build();
    }

    private static ProductRequestDto validProductRequest() {
        ProductRequestDto request = new ProductRequestDto();
        request.setSku("PROD-MOUSE-001");
        request.setName("Ergonomic Mouse");
        request.setDescription("Wireless mouse");
        request.setPrice(new BigDecimal("79.99"));
        request.setStockQuantity(30);
        return request;
    }
}