package dev.jahlopez.productcatalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(60);
    private static final Duration PRODUCTS_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCTS_LIST_TTL = Duration.ofMinutes(15);

    /**
     * Configures default Redis cache settings, including JSON serialization,
     * string-based keys, disabled null caching, and a 60-minute TTL.
     *
     * @return the default Redis cache configuration
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                jsonSerializer
                        )
                );
    }

    /**
     * Applies cache-specific TTLs while retaining the default serialization
     * settings and other cache configuration options.
     *
     * @return the Redis cache manager customizer
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            RedisCacheConfiguration cacheConfiguration
    ) {
        return builder -> builder
                .withCacheConfiguration(
                        "products",
                        cacheConfiguration.entryTtl(PRODUCTS_TTL)
                )
                .withCacheConfiguration(
                        "productsList",
                        cacheConfiguration.entryTtl(PRODUCTS_LIST_TTL)
                );
    }
}