package com.seap.smartfinancetracker.analytics.constant;

/**
 * Constants shared by the analytics caching machinery: the Redis cache names that back the
 * three read-only analytics endpoints, and the conventions used to build their cache keys.
 * <p>
 * Cache invalidation is version-based rather than key-targeted: each user has a monotonic
 * version counter stored under {@link #VERSION_KEY_PREFIX}, and every cache key folds in the
 * current version. Bumping the counter (on any transaction mutation) makes all of that user's
 * previously cached entries unreachable, after which they expire naturally via their TTL.
 * </p>
 */
public final class AnalyticsCacheConstant {

    private AnalyticsCacheConstant() {
    }

    /** Cache backing {@code AnalyticsService.getSpendingByCategory}. */
    public static final String SPENDING_BY_CATEGORY_CACHE = "analytics:spending-by-category";

    /** Cache backing {@code AnalyticsService.getPeriodSummary}. */
    public static final String SUMMARY_CACHE = "analytics:summary";

    /** Cache backing {@code AnalyticsService.getIncomeExpenseTrend}. */
    public static final String INCOME_EXPENSE_TREND_CACHE = "analytics:income-expense-trend";

    /** Prefix of the Redis key holding a user's analytics cache version counter. */
    public static final String VERSION_KEY_PREFIX = "analytics:ver:";

    /** Sentinel rendered into a cache key when a date bound of the requested window is absent. */
    public static final String NULL_BOUND = "ALL";
}
