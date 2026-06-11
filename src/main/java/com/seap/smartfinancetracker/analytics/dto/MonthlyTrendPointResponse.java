package com.seap.smartfinancetracker.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing one point in an income-versus-expense time series.
 * <p>
 * Each point corresponds to a single calendar month and carries that month's income,
 * expense, and net totals. Months in which the user had neither income nor expense are
 * omitted from the series rather than rendered as zero.
 * </p>
 *
 * @param year         the four-digit calendar year of this point
 * @param month        the calendar month of this point, in the range 1 (January) to 12 (December)
 * @param totalIncome  the total income recorded in this month
 * @param totalExpense the total expense recorded in this month
 * @param net          the net change for this month ({@code totalIncome - totalExpense}); may be negative
 */
@Builder
public record MonthlyTrendPointResponse(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net
) {
}
