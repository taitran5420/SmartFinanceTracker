package com.seap.smartfinancetracker.analytics.cache;

import com.seap.smartfinancetracker.analytics.constant.AnalyticsCacheConstant;
import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Coordinates version-based caching for the analytics endpoints.
 * <p>
 * Each user owns a monotonic version counter in Redis. {@link #buildKey} folds the current
 * version into every cache key, and {@link #invalidate} increments the counter so that all of
 * a user's existing analytics entries become unreachable at once — the only practical way to
 * evict entries whose keys embed an open-ended set of date windows. Orphaned entries are later
 * reclaimed by their TTL.
 * </p>
 * <p>
 * {@code buildKey} is referenced from the {@code @Cacheable} key expressions on
 * {@code AnalyticsServiceImpl}; {@code invalidate} is driven by the analytics cache eviction
 * aspect after a transaction mutation commits.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsCacheService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Builds the cache key for an analytics request, scoping it to the user, their current
     * cache version, and the requested window. Absent date bounds are rendered as
     * {@link AnalyticsCacheConstant#NULL_BOUND} so an unbounded window keys distinctly from a
     * bounded one.
     *
     * @param userId  the authenticated user's id
     * @param request the requested window (bounds may be {@code null})
     * @return a stable, version-scoped cache key
     */
    public String buildKey(UUID userId, AnalyticsPeriodRequest request) {
        long version = currentVersion(userId);
        String start = request.startDate() == null ? AnalyticsCacheConstant.NULL_BOUND : request.startDate().toString();
        String end = request.endDate() == null ? AnalyticsCacheConstant.NULL_BOUND : request.endDate().toString();
        return userId + ":v" + version + ":" + start + ":" + end;
    }

    /**
     * Invalidates every cached analytics entry for a user by incrementing their version
     * counter. Subsequent reads compute fresh values under the new version; the superseded
     * entries expire via their TTL.
     *
     * @param userId the user whose analytics caches should be invalidated
     */
    public void invalidate(UUID userId) {
        try {
            Long newVersion = redisTemplate.opsForValue().increment(versionKey(userId));
            log.debug("Invalidated analytics cache for user {} (version now {})", userId, newVersion);
        } catch (DataAccessException e) {
            // Best-effort: a failed bump only means entries linger until their TTL expires; it
            // must never roll back or break the transaction whose commit triggered this call.
            log.warn("Failed to invalidate analytics cache for user {}; entries will expire via TTL", userId, e);
        }
    }

    /**
     * Reads the user's current cache version, treating an absent counter as version {@code 0}.
     * A Redis failure falls back to the version-{@code 0} key; the ensuing cache read/write also
     * fails and is swallowed by the cache error handler, so the analytics query simply runs
     * against the database.
     *
     * @param userId the user whose version to read
     * @return the current version, or {@code 0} if none has been set or Redis is unavailable
     */
    private long currentVersion(UUID userId) {
        try {
            String version = redisTemplate.opsForValue().get(versionKey(userId));
            return version == null ? 0L : Long.parseLong(version);
        } catch (DataAccessException e) {
            log.warn("Failed to read analytics cache version for user {}; bypassing cache", userId, e);
            return 0L;
        }
    }

    private String versionKey(UUID userId) {
        return AnalyticsCacheConstant.VERSION_KEY_PREFIX + userId;
    }
}
