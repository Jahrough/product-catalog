package dev.jahlopez.productcatalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration for the product catalog API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Product Catalog API",
                version = "1.0.0",
                description = "REST API for managing products, SKU lookups, pricing, and inventory."
        ),
        servers = @Server(url = "/", description = "Current environment")
)
public class OpenApiConfig {
}
