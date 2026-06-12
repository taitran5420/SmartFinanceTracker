package com.seap.smartfinancetracker.budget.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object providing an aggregated summary of a budget.
 * <p>
 * This record is designed specifically for dashboard and reporting views.
 * It combines the static budget limits with dynamic transaction data,
 * pre-calculating key financial metrics to simplify client-side rendering.
 * </p>
 *
 * @param budgetId           the unique identifier of the budget
 * @param categoryId         the unique identifier of the associated category
 * @param categoryName       the human-readable name of the category (denormalized for convenience)
 * @param amountLimit        the maximum allowed spending limit set for this budget
 * @param spentAmount        the total amount already spent in this category for the budgeted period
 * @param remaining          the exact monetary amount left to spend ({@code amountLimit - spentAmount}).
 * Can be negative if the user has exceeded the budget.
 * @param progressPercentage the completion rate of the budget as a percentage
 * Values can exceed 100 if over budget.
 * @param isOverBudget       a boolean flag that is strictly {@code true} if {@code spentAmount}
 * exceeds {@code amountLimit}, otherwise {@code false}
 */
@Builder
public record BudgetSummaryResponse(
        UUID budgetId,
        UUID categoryId,
        String categoryName,
        BigDecimal amountLimit,
        BigDecimal spentAmount,
        BigDecimal remaining,
        double progressPercentage,
        boolean isOverBudget
) {
}
