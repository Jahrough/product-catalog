package dev.jahlopez.productcatalog.listener;

import dev.jahlopez.productcatalog.document.ProductDocument;
import dev.jahlopez.productcatalog.event.ProductEvent;
import dev.jahlopez.productcatalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Event-driven Kafka consumer responsible for processing product lifecycle events,
 * maintaining near real-time Elasticsearch search projections, and auditing DLQ failures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductSearchRepository productSearchRepository;

    private static final String POISON_PILL_SKU = "POISON-PILL-SKU";

    /**
     * Handles product events for general service business logic and poison-pill testing.
     *
     * @param event  the incoming product event payload
     * @param offset the Kafka message offset for tracking and auditing
     */
    @KafkaListener(
            topics = "${app.kafka.topic.product-events:product-events}",
            groupId = "${spring.kafka.consumer.group-id:product-catalog-group}"
    )
    public void handleProductEvent(ProductEvent event, @Header(name = "kafka_offset", defaultValue = "0") long offset) {
        if (event == null) {
            log.warn("Received null ProductEvent payload at offset: {}", offset);
            return;
        }

        log.info("Processing product event for SKU: {} at offset: {}", event.sku(), offset);

        if (POISON_PILL_SKU.equals(event.sku())) {
            log.error("Encountered poison-pill SKU: {}. Triggering exception for retry and DLQ routing.", event.sku());
            throw new RuntimeException("Simulated processing failure - triggering retry & DLQ");
        }

        log.info("Successfully processed product event for SKU: {}", event.sku());
    }

    /**
     * Handles product events specifically for near real-time Elasticsearch search indexing and synchronization.
     *
     * @param event the incoming product lifecycle event
     */
    @KafkaListener(
            topics = "${app.kafka.topic.product-events:product-events}",
            groupId = "product-catalog-search-group"
    )
    public void handleSearchIndexingEvent(ProductEvent event) {
        if (event == null || event.eventType() == null) {
            log.warn("Received invalid or null ProductEvent payload for search indexing.");
            return;
        }

        log.info("Indexing ProductEvent [{}] for SKU: {} in Elasticsearch", event.eventType(), event.sku());

        switch (event.eventType()) {
            case CREATED, UPDATED -> {
                ProductDocument document = mapToDocument(event);
                productSearchRepository.save(document);
                log.info("Successfully indexed product ID: {} in Elasticsearch", event.productId());
            }
            case DELETED -> {
                if (event.productId() != null) {
                    productSearchRepository.deleteById(event.productId());
                    log.info("Successfully removed product ID: {} from Elasticsearch", event.productId());
                } else {
                    log.warn("Cannot delete document from Elasticsearch: productId is null for SKU: {}", event.sku());
                }
            }
            default -> log.warn("Unhandled event type [{}] encountered for SKU: {}", event.eventType(), event.sku());
        }
    }

    /**
     * Dedicated consumer listener to monitor, audit, and log messages landing in the Dead Letter Queue (DLT).
     *
     * @param event        the failed product event payload recovered from the DLT
     * @param errorMessage the root-cause exception message associated with the failure
     */
    @KafkaListener(
            topics = "${app.kafka.topic.product-events:product-events}.DLT",
            groupId = "product-catalog-dlq-group"
    )
    public void handleProductEventDlq(
            ProductEvent event,
            @Header(name = "kafka_dlt-exception-message", defaultValue = "Unknown") String errorMessage) {
        String sku = (event != null && event.sku() != null) ? event.sku() : "UNKNOWN";
        log.error("⚠️ Dead Letter Queue Alert: Message for SKU '{}' failed processing. Reason: {}", sku, errorMessage);
    }

    /**
     * Maps a ProductEvent payload to an Elasticsearch ProductDocument projection.
     *
     * @param event the product event to map
     * @return the constructed ProductDocument
     */
    private ProductDocument mapToDocument(ProductEvent event) {
        return ProductDocument.builder()
                .id(event.productId())
                .sku(event.sku())
                .name(event.name())
                .description(event.description())
                .price(event.price())
                .stockQuantity(event.stockQuantity())
                .build();
    }
}