package dev.jahlopez.productcatalog.config;

import dev.jahlopez.productcatalog.event.ProductEvent;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Spring configuration class for setting up Kafka producer components.
 * This defines custom factories and templates specifically for producing ProductEvent messages.
 */
@Configuration
public class KafkaProducerConfig {

    // Spring Boot auto-configured Kafka properties (loaded from application.yml/properties)
    private final KafkaProperties kafkaProperties;

    /**
     * Constructor-based dependency injection for KafkaProperties.
     *
     * @param kafkaProperties properties automatically provided by Spring Boot Kafka auto-configuration
     */
    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Creates a ProducerFactory bean responsible for producing Kafka producer instances.
     * It builds the configuration map using Spring Boot's standard application properties.
     *
     * @return a ProducerFactory configured for String keys and ProductEvent values
     */
    @Bean
    public ProducerFactory<String, ProductEvent> productEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    /**
     * Creates a KafkaTemplate bean which provides high-level convenience methods
     * for sending messages to Kafka topics (e.g., kafkaTemplate.send(...)).
     *
     * @return a KafkaTemplate bound to the productEventProducerFactory
     */
    @Bean
    public KafkaTemplate<String, ProductEvent> productEventKafkaTemplate() {
        return new KafkaTemplate<>(productEventProducerFactory());
    }
}