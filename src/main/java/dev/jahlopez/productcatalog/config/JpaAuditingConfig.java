package dev.jahlopez.productcatalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Dedicated enterprise configuration for JPA entity auditing lifecycle management.
 * Supplies both the auditing activator and a robust AuditorAware provider for tracking
 * entity creation and modification actors across microservice execution contexts.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            // Enterprise Best Practice: Extract user/service principal from SecurityContext
            // or fallback to system service identifier for background/asynchronous worker threads.
            // Example: SecurityContextHolder.getContext().getAuthentication() ...
            return Optional.of("product-catalog-service");
        };
    }
}