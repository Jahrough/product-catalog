package dev.jahlopez.productcatalog.listener;

import dev.jahlopez.productcatalog.event.ProductEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    @KafkaListener(topics = "product-events", groupId = "product-catalog-group")
    public void handleProductEvent(ProductEvent event, @Header(name = "kafka_offset", defaultValue = "0") long offset) {
        log.info("Processing product event for SKU: {} at offset: {}", event.sku(), offset);

        // Simulate business logic failure for testing DLQ behavior
        if ("POISON-PILL-SKU".equals(event.sku())) {
            throw new RuntimeException("Simulated processing failure - triggering retry & DLQ");
        }

        // Normal processing logic...
        log.info("Successfully processed product event: {}", event.sku());
    }

    /**
     * Optional: Dedicated consumer listener to monitor and audit messages landing in the Dead Letter Queue.
     */
    @KafkaListener(topics = "product-events.DLT", groupId = "product-catalog-dlq-group")
    public void handleProductEventDlq(
            ProductEvent event,
            @Header(name = "kafka_dlt-exception-message", defaultValue = "Unknown") String errorMessage) {
        log.error("⚠️ Dead Letter Queue Alert: Message for SKU '{}' failed processing. Reason: {}",
                event != null ? event.sku() : "UNKNOWN", errorMessage);

        // Implement alert notifications (e.g., CloudWatch metrics, PagerDuty, or storage for manual replay)
    }
}