package com.fooddelivery.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 *  RedisConfig
 * ============================================================
 *
 *  Registers three beans:
 *
 *  1. LettuceConnectionFactory
 *     ─────────────────────────
 *     Reads host + port from application.properties via @Value.
 *     NO hardcoded values — switching to Docker / cloud Redis
 *     is a properties-only change.
 *     Lettuce is preferred over Jedis because it is:
 *       • non-blocking / async-ready (Netty-based)
 *       • thread-safe — single shared connection
 *       • the Spring Boot default since Boot 2.x
 *
 *  2. RedisTemplate<String, Object>
 *     ──────────────────────────────
 *     General-purpose template used for direct Redis operations
 *     (e.g., manual key inspection, pub/sub later).
 *     Serialisers:
 *       • Key   → StringRedisSerializer     (human-readable key)
 *       • Value → GenericJackson2JsonRedisSerializer  (JSON value)
 *
 *  3. RedisCacheManager
 *     ──────────────────
 *     Powers every @Cacheable / @CachePut / @CacheEvict annotation.
 *     Each cache gets an individual TTL from CacheConstants.
 *     Values are stored as JSON so they are:
 *       • Readable in redis-cli
 *       • Portable across JVM versions (no Java serialisation)
 *       • Debuggable without a Java deserialiser
 *
 *  @EnableCaching activates Spring's caching AOP proxy so that
 *  @Cacheable etc. on service methods actually intercept calls.
 * ============================================================
 */
@Configuration
@EnableCaching
public class RedisConfig {

    // Injected from application.properties — no hardcoding
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // ── 1. Connection Factory ────────────────────────────────────────

    /**
     * Creates a Lettuce connection to the Redis server.
     *
     * RedisStandaloneConfiguration wraps host + port so the factory
     * is agnostic of the transport (local / Docker / remote).
     * When you later add a password or TLS, add them here — nothing
     * else in the codebase changes.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    // ── 2. JSON Serialiser ───────────────────────────────────────────

    /**
     * Builds a Jackson ObjectMapper configured for Redis:
     *
     *  - JavaTimeModule          : handles LocalDateTime, Instant etc.
     *  - WRITE_DATES_AS_TIMESTAMPS false : ISO-8601 strings, not epoch longs
     *  - activateDefaultTyping   : embeds the Java class name as "@class"
     *    in every JSON payload so Spring can deserialise back to the
     *    correct type without needing the caller to specify it.
     *    LaissezFaireSubTypeValidator allows all types (safe in a
     *    private internal cache — NOT exposed to user input).
     */
    @Bean
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    // ── 3. RedisTemplate ────────────────────────────────────────────

    /**
     * A general-purpose RedisTemplate for direct Redis operations.
     * Not used by @Cacheable (the CacheManager handles that),
     * but useful for manual key inspection or future pub/sub work.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // ── 4. Cache Manager ────────────────────────────────────────────

    /**
     * Builds a RedisCacheManager where:
     *
     *  - The default configuration applies to any cache not listed
     *    explicitly (TTL = 10 min, JSON serialisation, key prefix enabled).
     *
     *  - Per-cache configurations override the TTL for the four
     *    named caches in CacheConstants.  All other settings (serialiser,
     *    prefix) are inherited from the default via entryTtl().
     *
     *  Key structure in Redis:
     *    zwigato:<cacheName>::<springGeneratedKey>
     *    e.g. zwigato:restaurants::1
     *         zwigato:categories::allActive
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {

        // ── Shared serialisation context ──
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        // ── Default cache config (fallback for any un-named cache) ──
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(CacheConstants.TTL_RESTAURANTS))
                .serializeValuesWith(jsonPair)
                .computePrefixWith(cacheName -> "zwigato:" + cacheName + "::");

        // ── Per-cache TTL overrides ──
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        cacheConfigs.put(
                CacheConstants.RESTAURANTS,
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.TTL_RESTAURANTS)));

        cacheConfigs.put(
                CacheConstants.FOOD_ITEMS,
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.TTL_FOOD_ITEMS)));

        cacheConfigs.put(
                CacheConstants.CATEGORIES,
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.TTL_CATEGORIES)));

        cacheConfigs.put(
                CacheConstants.USER_PROFILES,
                defaultConfig.entryTtl(Duration.ofSeconds(CacheConstants.TTL_USER_PROFILES)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()   // honours @Transactional — cache writes commit with TX
                .build();
    }
}
