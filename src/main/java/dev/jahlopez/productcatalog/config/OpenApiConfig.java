package dev.jahlopez.productcatalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Enterprise OpenAPI 3 configuration class for the product-catalog microservice.
 * Defines global API metadata, documentation contacts, server environments,
 * and global security schemes (Bearer Authentication).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Product Catalog Microservice API",
                version = "1.0.0",
                description = "Production-grade RESTful API for managing product catalog data, SKU lookups, pricing, and inventory operations with Redis caching and Spring Data JPA.",
                contact = @Contact(
                        name = "Jahrough Lopez",
                        email = "jahlopez81@gmail.com",
                        url = "https://github.com/Jahrough"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        ),
        servers = {
                @Server(url = "/", description = "Default Environment / Current Gateway Routing")
        }
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer token to access secured catalog endpoints"
)
public class OpenApiConfig {
}