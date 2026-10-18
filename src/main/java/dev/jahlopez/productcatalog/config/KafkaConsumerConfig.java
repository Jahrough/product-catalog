package dev.jahlopez.productcatalog.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    /**
     * Configures the Kafka listener container factory with a Dead Letter Queue (DLQ) recoverer
     * and a fixed backoff retry policy (e.g., 3 attempts with a 1-second delay).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> template) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Configure Dead Letter Publishing Recoverer to route failed messages to <topic>.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // Retry 3 times with a 1000ms (1 second) interval before sending to DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        // Optional: Add non-retryable exceptions if certain errors (like deserialization errors) should go straight to DLQ
        // errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        factory.setCommonErrorHandler(errorHandler);

        // Use manual acknowledgment if needed (optional)
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }
}