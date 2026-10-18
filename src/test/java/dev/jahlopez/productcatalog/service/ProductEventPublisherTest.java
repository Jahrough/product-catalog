package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.event.ProductEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ProductEventPublisher service.
 * Uses Mockito to isolate the publisher from the actual Kafka infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class ProductEventPublisherTest {

    // Mock the KafkaTemplate to avoid needing a real running Kafka broker during unit tests
    @Mock
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    // Inject the mocked KafkaTemplate into the ProductEventPublisher instance under test
    @InjectMocks
    private ProductEventPublisher productEventPublisher;

    /**
     * Verifies that publishing a product event successfully sends the expected message to Kafka.
     */
    @Test
    void publishProductEvent_ShouldSendEventToKafka() {
        // Arrange: Create a sample product event to be published
        ProductEvent event = ProductEvent.builder()
                .productId(1L)
                .sku("PROD-KEY-001")
                .name("Mechanical Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        // Mock a successful asynchronous completion future returned by KafkaTemplate.send()
        CompletableFuture<SendResult<String, ProductEvent>> future = CompletableFuture.completedFuture(mock(SendResult.class));

        // Stub the kafkaTemplate.send(...) method to return our completed future when called with specific arguments
        when(kafkaTemplate.send(eq("product-events"), eq("PROD-KEY-001"), any(ProductEvent.class))).thenReturn(future);

        // Act: Call the method under test
        productEventPublisher.publishProductEvent(event);

        // Assert: Verify that kafkaTemplate.send was called exactly once with the correct topic, key, and event payload
        verify(kafkaTemplate, times(1)).send(eq("product-events"), eq("PROD-KEY-001"), eq(event));
    }

    /**
     * Verifies that the publisher handles Kafka send failures gracefully without crashing or throwing uncaught exceptions.
     */
    @Test
    void publishProductEvent_WhenKafkaSendFails_ShouldHandleExceptionGracefully() {
        // Arrange: Create a sample product event
        ProductEvent event = ProductEvent.builder()
                .productId(1L)
                .sku("PROD-KEY-001")
                .name("Mechanical Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        // Create a CompletableFuture that completes exceptionally to simulate a broker failure
        CompletableFuture<SendResult<String, ProductEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        // Stub kafkaTemplate.send(...) to return the exceptionally completed future
        when(kafkaTemplate.send(eq("product-events"), eq("PROD-KEY-001"), any(ProductEvent.class))).thenReturn(failedFuture);

        // Act & Assert: Invoke the publisher and ensure no uncaught exceptions escape the asynchronous callback
        productEventPublisher.publishProductEvent(event);

        // Verify that the send attempt was still correctly made despite the downstream failure
        verify(kafkaTemplate, times(1)).send(eq("product-events"), eq("PROD-KEY-001"), eq(event));
    }

}