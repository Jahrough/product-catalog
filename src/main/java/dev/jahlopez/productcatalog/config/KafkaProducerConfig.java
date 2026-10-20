package dev.jahlopez.productcatalog.config;

import dev.jahlopez.productcatalog.event.ProductEvent;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration for Kafka producer components.
 * Configures factories and templates for producing ProductEvent messages.
 */
@Configuration
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final SslBundles sslBundles;

    public KafkaProducerConfig(KafkaProperties kafkaProperties, SslBundles sslBundles) {
        this.kafkaProperties = kafkaProperties;
        this.sslBundles = sslBundles;
    }

    /**
     * Creates a producer factory for String keys and ProductEvent values.
     *
     * @return configured Kafka producer factory
     */
    @Bean
    public ProducerFactory<String, ProductEvent> producerFactory() {
        // Build producer settings, including any configured SSL bundle properties.
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));

        // Create the factory used to manage Kafka producer instances.
        return new DefaultKafkaProducerFactory<>(properties);
    }

    /**
     * Creates a KafkaTemplate for sending ProductEvent messages.
     *
     * @param producerFactory configured producer factory
     * @return Kafka template for ProductEvent messages
     */
    @Bean
    public KafkaTemplate<String, ProductEvent> kafkaTemplate(ProducerFactory<String, ProductEvent> producerFactory) {
        // Reuse the configured factory for all KafkaTemplate operations.
        return new KafkaTemplate<>(producerFactory);
    }
}