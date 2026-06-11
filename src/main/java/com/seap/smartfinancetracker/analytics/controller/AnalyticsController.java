package com.seap.smartfinancetracker.analytics.controller;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import com.seap.smartfinancetracker.analytics.dto.MonthlyTrendPointResponse;
import com.seap.smartfinancetracker.analytics.dto.PeriodSummaryResponse;
import com.seap.smartfinancetracker.analytics.dto.SpendingByCategoryResponse;
import com.seap.smartfinancetracker.analytics.service.AnalyticsService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller exposing read-only financial analytics over the authenticated user's
 * transactions.
 * <p>
 * Every endpoint is strictly scoped to the caller via the {@link CurrentUserId} annotation
 * and accepts an optional date-range window bound from query parameters
 * ({@code startDate}, {@code endDate}). Omitting a bound widens the window on that side;
 * omitting both aggregates over the user's entire history.
 * </p>
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Returns the user's expenses broken down by category for the given window.
     *
     * @param userId  the authenticated user's ID, automatically injected from the security context
     * @param request the optional date-range window bound from query parameters
     * @return a {@link ResponseEntity} containing the per-category spending breakdown
     */
    @GetMapping("/spending-by-category")
    public ResponseEntity<SpendingByCategoryResponse> getSpendingByCategory(
            @CurrentUserId UUID userId,
            @ModelAttribute AnalyticsPeriodRequest request) {
        return ResponseEntity.ok(analyticsService.getSpendingByCategory(userId, request));
    }

    /**
     * Returns a high-level financial summary (totals, net, transaction count, and the top
     * spending category) for the given window.
     *
     * @param userId  the authenticated user's ID
     * @param request the optional date-range window bound from query parameters
     * @return a {@link ResponseEntity} containing the period summary
     */
    @GetMapping("/summary")
    public ResponseEntity<PeriodSummaryResponse> getPeriodSummary(
            @CurrentUserId UUID userId,
            @ModelAttribute AnalyticsPeriodRequest request) {
        return ResponseEntity.ok(analyticsService.getPeriodSummary(userId, request));
    }

    /**
     * Returns a month-by-month income-versus-expense time series for the given window.
     *
     * @param userId  the authenticated user's ID
     * @param request the optional date-range window bound from query parameters
     * @return a {@link ResponseEntity} containing the chronologically ordered trend points
     */
    @GetMapping("/income-expense-trend")
    public ResponseEntity<List<MonthlyTrendPointResponse>> getIncomeExpenseTrend(
            @CurrentUserId UUID userId,
            @ModelAttribute AnalyticsPeriodRequest request) {
        return ResponseEntity.ok(analyticsService.getIncomeExpenseTrend(userId, request));
    }
}
