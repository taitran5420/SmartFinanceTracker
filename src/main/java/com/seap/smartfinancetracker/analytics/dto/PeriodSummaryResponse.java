package com.seap.smartfinancetracker.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object providing a high-level financial overview for an analytics window.
 * <p>
 * Intended to populate a dashboard header, it combines aggregate totals with the single
 * heaviest expense category for the period. When the user has no expenses in the window,
 * the {@code topCategory*} fields are {@code null}/{@code 0}.
 * </p>
 *
 * @param startDate         the inclusive start of the window, echoed back from the request ({@code null} if unbounded)
 * @param endDate           the inclusive end of the window, echoed back from the request ({@code null} if unbounded)
 * @param totalIncome       the sum of all income transactions in the period
 * @param totalExpense      the sum of all expense transactions in the period
 * @param net               the net change in the period ({@code totalIncome - totalExpense}); may be negative
 * @param transactionCount  the number of active transactions in the period
 * @param topCategoryId     the identifier of the category with the highest spend, or {@code null} if none
 * @param topCategoryName   the name of the highest-spending category, or {@code null} if none
 * @param topCategoryAmount the amount spent in the highest-spending category, or {@code 0} if none
 */
@Builder
public record PeriodSummaryResponse(
        Instant startDate,
        Instant endDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net,
        long transactionCount,
        UUID topCategoryId,
        String topCategoryName,
        BigDecimal topCategoryAmount
) {
}
