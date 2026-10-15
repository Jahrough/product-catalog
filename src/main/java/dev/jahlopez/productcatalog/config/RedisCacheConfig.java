package dev.jahlopez.productcatalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures Redis caching for the Product Catalog service.
 *
 * Enables Spring's caching support and configures Redis to serialize
 * cache keys as strings and cache values as JSON.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Defines the default serialization settings for Redis cache entries.
     *
     * Uses StringRedisSerializer for cache keys to keep them human-readable
     * and GenericJackson2JsonRedisSerializer for cache values to support
     * JSON serialization and deserialization of cached Java objects.
     *
     * @param objectMapper Spring Boot's configured Jackson ObjectMapper
     * @return the default Redis cache configuration
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration(
            ObjectMapper objectMapper
    ) {
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper.copy());

        return RedisCacheConfiguration.defaultCacheConfig()
                // Serialize cache keys as strings instead of binary data.
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                // Serialize cached objects as JSON with type information.
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(valueSerializer)
                );
    }

    /**
     * Creates the Redis-backed Spring Cache manager.
     *
     * The connection factory provides access to the configured Redis server.
     * The supplied cache configuration ensures cache entries use the
     * serializers defined above.
     *
     * @param connectionFactory the Redis connection factory
     * @param redisCacheConfiguration the default cache serialization settings
     * @return the configured Redis cache manager
     */
    @Bean
    CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration redisCacheConfiguration
    ) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .build();
    }
}