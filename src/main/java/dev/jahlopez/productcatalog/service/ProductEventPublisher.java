package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.event.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing product lifecycle events (ProductEvent) to the Kafka event stream
 * asynchronously with reliable callback handling and message partitioning key routing.
 */
@Slf4j
@Service
public class ProductEventPublisher {

    private final KafkaTemplate<String, ProductEvent> productEventKafkaTemplate;
    private final String topic;

    /**
     * Constructs the ProductEventPublisher with the required Kafka template and topic configuration.
     *
     * @param productEventKafkaTemplate template configured for product events
     * @param topic                     the target Kafka topic resolved from properties or defaults
     */
    public ProductEventPublisher(
            KafkaTemplate<String, ProductEvent> productEventKafkaTemplate,
            @Value("${app.kafka.topic.product-events:product-events}") String topic) {
        this.productEventKafkaTemplate = productEventKafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publishes a product event to the configured Kafka topic asynchronously.
     * Uses the product's SKU as the message key to ensure ordered processing for identical products.
     *
     * @param event the ProductEvent payload to publish
     */
    public void publishProductEvent(ProductEvent event) {
        if (event == null || event.sku() == null) {
            log.error("Attempted to publish a null ProductEvent or an event with a null SKU.");
            throw new IllegalArgumentException("ProductEvent and SKU must not be null for publishing.");
        }

        log.info("Publishing ProductEvent [{}] for SKU: {} to topic: {}", event.eventType(), event.sku(), topic);

        // Send the message asynchronously, using event.sku() as the partition key
        CompletableFuture<SendResult<String, ProductEvent>> future = productEventKafkaTemplate.send(topic, event.sku(), event);

        // Register a callback to handle the asynchronous result (success or failure) once the broker acknowledges it
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent ProductEvent for SKU: {} to partition: {} with offset: {}",
                        event.sku(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send ProductEvent for SKU: {} due to: {}", event.sku(), ex.getMessage(), ex);
            }
        });
    }
}