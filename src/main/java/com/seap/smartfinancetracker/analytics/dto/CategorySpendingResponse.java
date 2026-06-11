package com.seap.smartfinancetracker.analytics.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing the expense total for a single category within an
 * analytics window.
 * <p>
 * Designed for chart rendering (e.g. pie or bar charts), it carries both the absolute
 * {@code totalSpent} and its {@code percentage} share of total expenses for the period.
 * </p>
 *
 * @param categoryId   the unique identifier of the category
 * @param categoryName the human-readable name of the category (denormalized for convenience)
 * @param totalSpent   the total expense amount recorded against this category for the period
 * @param percentage   this category's share of total period expenses, as a percentage (0&ndash;100)
 */
@Builder
public record CategorySpendingResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal totalSpent,
        double percentage
) {
}
