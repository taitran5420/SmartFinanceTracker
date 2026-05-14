package com.seap.smartfinancetracker.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for creating a new financial budget.
 * <p>
 * This record encapsulates the payload required to set a spending limit
 * for a specific category within a designated timeframe (month and year).
 * </p>
 *
 * @param categoryId  the unique identifier of the category to be budgeted
 * @param amountLimit the maximum monetary amount allowed to be spent; must be strictly positive
 * @param month       the target month for the budget (must be between 1 and 12)
 * @param year        the target year for the budget
 */
public record BudgetCreateRequest(
        @NotNull(message = "Category cannot be null")
        UUID categoryId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be strictly positive")
        BigDecimal amountLimit,

        @NotNull(message = "Month cannot be null")
        @Min(1) @Max(12)
        Integer month,

        @NotNull(message = "Year cannot be null")
        Integer year
) {
}
