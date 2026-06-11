package com.seap.smartfinancetracker.analytics.service;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import com.seap.smartfinancetracker.analytics.dto.MonthlyTrendPointResponse;
import com.seap.smartfinancetracker.analytics.dto.PeriodSummaryResponse;
import com.seap.smartfinancetracker.analytics.dto.SpendingByCategoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for read-only financial analytics derived from a user's transactions.
 * <p>
 * Every method is strictly scoped to the authenticated user and accepts an optional
 * {@link AnalyticsPeriodRequest} window. Implementations must reject windows whose
 * {@code startDate} is after their {@code endDate}.
 * </p>
 */
public interface AnalyticsService {

    /**
     * Aggregates the user's expenses by category for the given window.
     *
     * @param userId  the unique identifier of the authenticated user
     * @param request the optional date-range window
     * @return the per-category spending breakdown, ordered by descending spend
     */
    SpendingByCategoryResponse getSpendingByCategory(UUID userId, AnalyticsPeriodRequest request);

    /**
     * Computes a high-level financial summary (totals, net, transaction count, and the
     * top spending category) for the given window.
     *
     * @param userId  the unique identifier of the authenticated user
     * @param request the optional date-range window
     * @return the aggregated period summary
     */
    PeriodSummaryResponse getPeriodSummary(UUID userId, AnalyticsPeriodRequest request);

    /**
     * Builds a month-by-month income-versus-expense time series for the given window.
     *
     * @param userId  the unique identifier of the authenticated user
     * @param request the optional date-range window
     * @return a chronologically ordered list of monthly trend points
     */
    List<MonthlyTrendPointResponse> getIncomeExpenseTrend(UUID userId, AnalyticsPeriodRequest request);
}
