package com.seap.smartfinancetracker.common.config;

import com.seap.smartfinancetracker.analytics.constant.AnalyticsCacheConstant;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

/**
 * Enables Spring's caching abstraction and wires the Redis-backed {@link RedisCacheManager}
 * used by the analytics endpoints.
 * <p>
 * Only the three analytics caches are configured here, all sharing a five-minute TTL. The TTL
 * is a backstop: correctness is maintained by explicit version-bump invalidation on every
 * transaction mutation (see the analytics cache eviction aspect), while the TTL bounds key
 * growth in Redis and reclaims entries orphaned by a version bump.
 * </p>
 * <p>
 * Caching is best-effort: the {@link #errorHandler()} swallows (and logs) Redis failures so a
 * cache outage degrades the analytics endpoints to direct database reads rather than failing
 * them — the same "a cache miss must never break the business operation" philosophy applied to
 * messaging elsewhere in the codebase.
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * Backstop time-to-live for analytics cache entries. Short enough that any missed
     * invalidation self-heals quickly, long enough to absorb dashboard read bursts.
     */
    private static final Duration ANALYTICS_TTL = Duration.ofMinutes(5);

    /**
     * Builds the Redis cache manager, registering the three analytics caches with a shared
     * five-minute TTL and a JSON value serializer.
     *
     * @param connectionFactory the Redis connection factory auto-configured by Spring Boot
     * @return a {@link RedisCacheManager} scoped to the analytics caches
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration analyticsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ANALYTICS_TTL)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()));

        Map<String, RedisCacheConfiguration> caches = Map.of(
                AnalyticsCacheConstant.SPENDING_BY_CATEGORY_CACHE, analyticsConfig,
                AnalyticsCacheConstant.SUMMARY_CACHE, analyticsConfig,
                AnalyticsCacheConstant.INCOME_EXPENSE_TREND_CACHE, analyticsConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(analyticsConfig)
                .withInitialCacheConfigurations(caches)
                .build();
    }

    /**
     * Serializer used for cached values. Built on the Jackson 3 generic serializer
     * ({@code GenericJackson2JsonRedisSerializer} is deprecated in spring-data-redis 4), it
     * relies on Jackson 3's built-in {@code java.time} support so {@code Instant} bounds
     * round-trip as ISO-8601 strings, and enables default typing — embedding the type hints
     * required to deserialize the analytics DTOs, including the {@code List} returned by the
     * income/expense trend endpoint. {@code enableSpringCacheNullValueSupport} lets Spring's
     * {@code NullValue} marker round-trip. Default typing is scoped to JDK and application
     * types rather than left fully open. Jackson 3 writes dates as ISO-8601 strings by default,
     * so no explicit date-format configuration is required.
     *
     * @return a JSON {@link RedisSerializer} for arbitrary analytics response objects
     */
    private RedisSerializer<Object> valueSerializer() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("java.")
                .allowIfSubType("com.seap.smartfinancetracker.")
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .enableSpringCacheNullValueSupport()
                .build();
    }

    /**
     * Logs and swallows cache get/put/evict/clear failures (e.g. a Redis outage) instead of
     * propagating them, so analytics requests fall back to the database rather than erroring.
     *
     * @return a {@link CacheErrorHandler} that never rethrows
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
