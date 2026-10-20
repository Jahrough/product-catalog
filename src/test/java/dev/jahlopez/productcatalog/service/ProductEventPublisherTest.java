package dev.jahlopez.productcatalog.service;

import dev.jahlopez.productcatalog.event.ProductEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
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
 * Unit test suite for the ProductEventPublisher service.
 * Utilizes Mockito to isolate the publisher from the actual Kafka infrastructure.
 */
@ExtendWith(MockitoExtension.class)
class ProductEventPublisherTest {

    private static final String TEST_SKU = "PROD-KEY-001";

    @Mock
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @InjectMocks
    private ProductEventPublisher productEventPublisher;

    @Mock
    private SendResult<String, ProductEvent> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    @Test
    @DisplayName("Should successfully publish product event to Kafka and process success callback")
    void publishProductEvent_ShouldSendEventToKafka() {
        // Arrange: Create a sample product event to be published
        ProductEvent event = ProductEvent.builder()
                .productId(1L)
                .sku(TEST_SKU)
                .name("Mechanical Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        // Stub mock metadata to prevent NullPointerException during callback logging
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(100L);

        // Mock a successful asynchronous completion future returned by KafkaTemplate.send()
        CompletableFuture<SendResult<String, ProductEvent>> future = CompletableFuture.completedFuture(sendResult);

        // Stub using any() for the uninitialized topic field (which is null in pure Mockito unit tests)
        when(kafkaTemplate.send(any(), eq(TEST_SKU), any(ProductEvent.class))).thenReturn(future);

        // Act: Call the method under test
        productEventPublisher.publishProductEvent(event);

        // Assert: Verify that kafkaTemplate.send was called exactly once with the correct parameters
        verify(kafkaTemplate, times(1)).send(any(), eq(TEST_SKU), eq(event));
    }

    @Test
    @DisplayName("Should handle Kafka broker transmission failure gracefully in asynchronous callback")
    void publishProductEvent_WhenKafkaSendFails_ShouldHandleExceptionGracefully() {
        // Arrange: Create a sample product event
        ProductEvent event = ProductEvent.builder()
                .productId(1L)
                .sku(TEST_SKU)
                .name("Mechanical Keyboard")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(50)
                .eventType(ProductEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();

        // Create a CompletableFuture that completes exceptionally to simulate a broker failure
        CompletableFuture<SendResult<String, ProductEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

        // Stub using any() for the topic parameter
        when(kafkaTemplate.send(any(), eq(TEST_SKU), any(ProductEvent.class))).thenReturn(failedFuture);

        // Act: Invoke the publisher
        productEventPublisher.publishProductEvent(event);

        // Assert: Verify that the send attempt was correctly made
        verify(kafkaTemplate, times(1)).send(any(), eq(TEST_SKU), eq(event));
    }
}