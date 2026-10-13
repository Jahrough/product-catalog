package dev.jahlopez.productcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point and bootstrap class for the Product Catalog microservice.
 * Initializes the Spring Boot application context, auto-configuration bindings,
 * component scanning, and embedded servlet container.
 */
@SpringBootApplication
public class ProductCatalogApplication {

    /**
     * Main application method that launches the Spring Boot runtime.
     *
     * @param args command-line arguments passed during application startup
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogApplication.class, args);
    }
}