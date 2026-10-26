package dev.jahlopez.productcatalog.config;

import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * Configures Redis-backed caching for product catalog read operations.
 *
 * <p>This configuration defines cache names, expiration policies (TTL),
 * and serialization rules for individual products and product collections.
 * Caching reduces repeated database queries and can improve response latency
 * for frequently accessed product data.</p>
 *
 * <p>Redis is a performance optimization, not the system of record.
 * PostgreSQL remains authoritative for product data.</p>
 *
 * <p>The cache names defined here must match the names used by application
 * cache annotations such as {@code @Cacheable}, {@code @CachePut}, and
 * {@code @CacheEvict}.</p>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /*
     * Centralized cache names prevent spelling inconsistencies between
     * this configuration and cache annotations in service classes.
     */
    public static final String PRODUCTS_BY_ID_CACHE = "productsById";
    public static final String PRODUCTS_BY_SKU_CACHE = "productsBySku";
    public static final String PRODUCTS_LIST_CACHE = "productsList";

    /*
     * Fallback expiration policy for caches that use the default configuration.
     * A TTL limits how long an entry remains in Redis before it expires.
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(60);

    /*
     * Individual product lookups use a 30-minute TTL.
     * These policies can be tuned independently based on access patterns
     * and how quickly product data needs to become visible after updates.
     */
    private static final Duration PRODUCTS_BY_ID_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCTS_BY_SKU_TTL = Duration.ofMinutes(30);

    /*
     * Product lists use a shorter TTL because collection results may be
     * affected by product creation, deletion, or changes to searchable fields.
     */
    private static final Duration PRODUCTS_LIST_TTL = Duration.ofMinutes(15);

    /**
     * Creates the JSON mapper used by Redis cache serializers.
     *
     * <p>The mapper converts Java objects into JSON for storage and reconstructs
     * Java objects when cached values are read. Keeping it as a bean allows
     * the same mapper configuration to be reused by cache serializers.</p>
     *
     * <p>If the application has a centrally configured Jackson mapper with
     * custom modules or serialization rules, consider reusing that mapper
     * to keep cache serialization consistent with the rest of the application.</p>
     *
     * @return the JSON mapper used for Redis cache serialization
     */
    @Bean
    public JsonMapper redisCacheJsonMapper() {
        return JsonMapper.builder().build();
    }

    /**
     * Defines the default Redis cache configuration.
     *
     * <p>Cache keys are stored as strings, while values are serialized as JSON
     * representations of {@link ProductResponseDto} objects. The default TTL
     * is 60 minutes unless a named cache overrides it.</p>
     *
     * <p>Null values are disabled to avoid caching absent results implicitly.
     * This means repeated requests for nonexistent products may still trigger
     * database queries unless explicit negative caching is implemented.</p>
     *
     * @param mapper JSON mapper used to serialize and deserialize cached values
     * @return the default Redis cache configuration
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration(JsonMapper mapper) {

        // Configure JSON serialization for individual product response DTOs.
        var productSerializer = new JacksonJsonRedisSerializer<>(mapper, ProductResponseDto.class);

        return RedisCacheConfiguration.defaultCacheConfig()
                                      .entryTtl(DEFAULT_TTL)
                                      .disableCachingNullValues()
                                      .serializeKeysWith(
                                              RedisSerializationContext
                                                      .SerializationPair
                                                      .fromSerializer(new StringRedisSerializer())
                                      )
                                      .serializeValuesWith(
                                              RedisSerializationContext
                                                      .SerializationPair
                                                      .fromSerializer(productSerializer)
                                      );
    }

    /**
     * Registers cache-specific expiration policies and serializers.
     *
     * <p>The product-by-ID and product-by-SKU caches store individual
     * {@link ProductResponseDto} instances. The product-list cache stores
     * a {@code List<ProductResponseDto>} and therefore requires a serializer
     * configured for the collection type.</p>
     *
     * <p>All three caches use string keys and disable null-value caching.
     * Their TTLs override the default TTL defined by {@link #cacheConfiguration(JsonMapper)}.</p>
     *
     * @param mapper JSON mapper used by the cache serializers
     * @return a customizer that registers the named cache configurations
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(JsonMapper mapper) {

        // Serializer for a single product response.
        var productSerializer = new JacksonJsonRedisSerializer<>(mapper, ProductResponseDto.class);

        /*
         * Preserve the collection's element type during deserialization.
         * Using an explicit collection type avoids treating the cached list
         * as an untyped collection of generic maps.
         */
        var productListType = mapper.getTypeFactory()
                                    .constructCollectionType(List.class, ProductResponseDto.class);

        // Serializer for a list of product response DTOs.
        var productListSerializer = new JacksonJsonRedisSerializer<>(mapper, productListType);

        /*
         * Define common settings once so each cache uses the same key format
         * and null-value policy. Cache-specific serializers and TTLs are added
         * below without duplicating these shared settings.
         */
        var baseCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                     .disableCachingNullValues()
                                                     .serializeKeysWith(
                                                             RedisSerializationContext
                                                                     .SerializationPair
                                                                     .fromSerializer(new StringRedisSerializer())
                                                     );

        // Individual product caches share the single-product serializer.
        var productCacheConfig = baseCacheConfig.serializeValuesWith(
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(productSerializer)
        );

        // Collection cache uses its dedicated list serializer.
        var productListCacheConfig = baseCacheConfig.serializeValuesWith(
                RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(productListSerializer)
        );

        /*
         * Register each named cache with its own expiration policy.
         * These names must match the cache names used in service annotations.
         */
        return builder -> builder.withCacheConfiguration(PRODUCTS_BY_ID_CACHE, productCacheConfig.entryTtl(PRODUCTS_BY_ID_TTL))
                        .withCacheConfiguration(PRODUCTS_BY_SKU_CACHE, productCacheConfig.entryTtl(PRODUCTS_BY_SKU_TTL))
                        .withCacheConfiguration(PRODUCTS_LIST_CACHE, productListCacheConfig.entryTtl(PRODUCTS_LIST_TTL));
    }
}