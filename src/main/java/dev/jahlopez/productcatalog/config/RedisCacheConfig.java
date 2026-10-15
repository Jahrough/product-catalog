package dev.jahlopez.productcatalog.config;

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
     * @return the default Redis cache configuration
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                // Serialize cache keys as strings instead of binary data.
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                // Serialize cached objects as JSON.
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(
                                        new GenericJackson2JsonRedisSerializer()
                                )
                );
    }

    /**
     * Creates the Redis-backed cache manager used by Spring's caching
     * abstraction.
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