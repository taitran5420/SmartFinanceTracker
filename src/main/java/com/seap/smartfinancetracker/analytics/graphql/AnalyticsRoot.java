package com.seap.smartfinancetracker.analytics.graphql;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;

import java.util.UUID;

/**
 * Lightweight source object returned by the top-level {@code analytics} query and passed to
 * the {@code Analytics} field resolvers.
 * <p>
 * It carries only the authenticated {@code userId} and the requested window; the actual
 * aggregation is deferred to the per-field {@code @SchemaMapping} resolvers, which run only
 * when their sub-field is selected. Its own components are not exposed in the schema — the
 * {@code Analytics} type declares only {@code summary}, {@code spendingByCategory} and
 * {@code incomeExpenseTrend}, all resolved by methods.
 * </p>
 *
 * @param userId the authenticated user's id, used to scope every analytics query
 * @param period the requested date window (bounds may be {@code null})
 */
record AnalyticsRoot(UUID userId, AnalyticsPeriodRequest period) {
}
