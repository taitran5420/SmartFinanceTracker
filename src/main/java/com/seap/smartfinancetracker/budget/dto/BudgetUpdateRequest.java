package com.seap.smartfinancetracker.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Data Transfer Object for updating an existing financial budget.
 * <p>
 * This record encapsulates the payload used to modify a budget's details.
 * <b>Business Rule:</b> Core attributes such as the target category, month,
 * and year are strictly immutable after creation. Therefore, this request
 * only exposes the mutable spending limit.
 * </p>
 *
 * @param amountLimit the updated maximum monetary amount allowed to be spent;
 * must be provided and strictly positive
 */
public record BudgetUpdateRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be strictly positive")
        BigDecimal amountLimit) {
}
