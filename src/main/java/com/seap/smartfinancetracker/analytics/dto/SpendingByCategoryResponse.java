package com.seap.smartfinancetracker.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object aggregating a user's expenses broken down by category for an
 * analytics window.
 * <p>
 * The {@code categories} list is ordered from highest to lowest spend, and each entry's
 * {@code percentage} is computed relative to {@code totalExpense}. When the user has no
 * expenses in the period, {@code totalExpense} is {@code 0} and {@code categories} is empty.
 * </p>
 *
 * @param totalExpense the sum of all expenses across every category for the period
 * @param categories   the per-category breakdown, ordered by descending spend
 */
@Builder
public record SpendingByCategoryResponse(
        BigDecimal totalExpense,
        List<CategorySpendingResponse> categories
) {
}
