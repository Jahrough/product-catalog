package dev.jahlopez.productcatalog.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * <p>Enables Spring's caching support and defines JSON serialization
 * for cached values and string serialization for cache keys.</p>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Defines the default serialization settings for Redis cache entries.
     *
     * <p>Configures a dedicated Jackson {@link ObjectMapper} with Java time
     * support and restricted polymorphic type handling. This allows cached
     * application objects to be deserialized into their original types.</p>
     *
     * @return the default Redis cache configuration
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        BasicPolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("dev.jahlopez.productcatalog.")
                        .build();

        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        redisObjectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                // Serialize cache keys as strings.
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
     * <p>Uses the supplied Redis connection factory and default cache
     * configuration to manage serialized cache entries.</p>
     *
     * @param connectionFactory the Redis connection factory
     * @param redisCacheConfiguration the default Redis cache configuration
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