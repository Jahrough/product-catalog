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

@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(60);
    private static final Duration PRODUCTS_BY_ID_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCTS_BY_SKU_TTL = Duration.ofMinutes(30);
    private static final Duration PRODUCTS_LIST_TTL = Duration.ofMinutes(15);

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        var mapper = JsonMapper.builder().build();

        var serializer = new JacksonJsonRedisSerializer<>(
                mapper, ProductResponseDto.class);

        return RedisCacheConfiguration.defaultCacheConfig()
                                      .entryTtl(DEFAULT_TTL)
                                      .disableCachingNullValues()
                                      .serializeKeysWith(
                                              RedisSerializationContext.SerializationPair.fromSerializer(
                                                      new StringRedisSerializer()))
                                      .serializeValuesWith(
                                              RedisSerializationContext.SerializationPair.fromSerializer(
                                                      serializer));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        var mapper = JsonMapper.builder().build();

        var productSerializer = new JacksonJsonRedisSerializer<>(
                mapper, ProductResponseDto.class);

        var listType = mapper.getTypeFactory()
                             .constructCollectionType(List.class, ProductResponseDto.class);

        var listSerializer = new JacksonJsonRedisSerializer<>(
                mapper, listType);

        var productCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                        .disableCachingNullValues()
                                                        .serializeKeysWith(
                                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        new StringRedisSerializer()))
                                                        .serializeValuesWith(
                                                                RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        productSerializer));

        var listCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                                                     .disableCachingNullValues()
                                                     .serializeKeysWith(
                                                             RedisSerializationContext.SerializationPair.fromSerializer(
                                                                     new StringRedisSerializer()))
                                                     .serializeValuesWith(
                                                             RedisSerializationContext.SerializationPair.fromSerializer(
                                                                     listSerializer));

        return builder -> builder
                .withCacheConfiguration(
                        "productsById",
                        productCacheConfig.entryTtl(PRODUCTS_BY_ID_TTL))
                .withCacheConfiguration(
                        "productsBySku",
                        productCacheConfig.entryTtl(PRODUCTS_BY_SKU_TTL))
                .withCacheConfiguration(
                        "productsList",
                        listCacheConfig.entryTtl(PRODUCTS_LIST_TTL));
    }
}