package dev.jahlopez.productcatalog.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private TestController testController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Should return 404 Not Found when ProductNotFoundException is thrown")
    void shouldHandleProductNotFoundException() throws Exception {
        Long missingId = 99L;
        when(productService.getProductById(missingId))
                .thenThrow(new ProductNotFoundException("Product not found with id: " + missingId));

        mockMvc.perform(get("/test/products/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found with id: " + missingId));
    }

    @Test
    @DisplayName("Should return 400 Bad Request on validation failure")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        ProductRequestDto invalidRequest = new ProductRequestDto();
        invalidRequest.setSku("");
        invalidRequest.setName("");
        invalidRequest.setPrice(new BigDecimal("-10.00"));

        mockMvc.perform(post("/test/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_PAYLOAD"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error on generic exception")
    void shouldHandleGenericException() throws Exception {
        when(productService.getProductById(any()))
                .thenThrow(new RuntimeException("Unexpected DB failure"));

        mockMvc.perform(get("/test/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    @RestController
    @RequestMapping("/test/products")
    static class TestController {

        private final ProductService productService;

        TestController(ProductService productService) {
            this.productService = productService;
        }

        @GetMapping("/{id}")
        public void getProductById(@PathVariable Long id) {
            productService.getProductById(id);
        }

        @PostMapping
        public void createProduct(@jakarta.validation.Valid @RequestBody ProductRequestDto requestDto) {
            productService.createProduct(requestDto);
        }
    }
}