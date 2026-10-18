package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing product lifecycle events (ProductEvent) to the Kafka event stream.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventPublisher {

    // Kafka template configured specifically for publishing ProductEvent values with String keys
    private final KafkaTemplate<String, ProductEvent> productEventKafkaTemplate;

    // The Kafka topic name where all product-related events are sent
    private static final String TOPIC = "product-events";

    /**
     * Publishes a product event to the Kafka topic asynchronously.
     * Uses the product's SKU as the message key to ensure ordered processing for the same product.
     *
     * @param event the ProductEvent payload to publish
     */
    public void publishProductEvent(ProductEvent event) {
        log.info("Publishing ProductEvent [{}] for SKU: {}", event.eventType(), event.sku());

        // Send the message asynchronously, using event.sku() as the partition key
        CompletableFuture<SendResult<String, ProductEvent>> future = productEventKafkaTemplate.send(TOPIC, event.sku(), event);

        // Register a callback to handle the asynchronous result (success or failure) once the broker acknowledges it
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Success: Log the partition and offset where the event was safely stored
                log.info("Successfully sent ProductEvent for SKU: {} to partition: {} with offset: {}",
                        event.sku(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // Failure: Log the error details if the broker is unreachable or publishing fails
                log.error("Failed to send ProductEvent for SKU: {} due to: {}", event.sku(), ex.getMessage(), ex);
            }
        });
    }
}