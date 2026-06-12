package com.seap.smartfinancetracker.budget.dto;

import com.seap.smartfinancetracker.budget.constant.BudgetValidationMessage;
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
        @NotNull(message = BudgetValidationMessage.CATEGORY_REQUIRED)
        UUID categoryId,

        @NotNull(message = BudgetValidationMessage.AMOUNT_REQUIRED)
        @Positive(message = BudgetValidationMessage.AMOUNT_POSITIVE)
        BigDecimal amountLimit,

        @NotNull(message = BudgetValidationMessage.MONTH_REQUIRED)
        @Min(value = 1, message = BudgetValidationMessage.MONTH_MIN)
        @Max(value = 12, message = BudgetValidationMessage.MONTH_MAX)
        Integer month,

        @NotNull(message = BudgetValidationMessage.YEAR_REQUIRED)
        Integer year
) {
}
