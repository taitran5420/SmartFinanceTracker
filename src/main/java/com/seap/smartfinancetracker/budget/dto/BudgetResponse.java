package com.seap.smartfinancetracker.budget.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for exposing financial budget details in API responses.
 * <p>
 * This record encapsulates the full state of a budget entity, providing clients
 * with comprehensive information including its identification, associated timeframe,
 * spending limits, and current lifecycle status (active vs. soft-deleted).
 * </p>
 *
 * @param id          the unique identifier of the budget
 * @param categoryId  the unique identifier of the category this budget applies to
 * @param amountLimit the maximum monetary amount allocated for this budget
 * @param month       the target month of the budget (1-12)
 * @param year        the target year of the budget
 * @param createdAt   the exact timestamp when the budget record was created
 * @param updatedAt   the exact timestamp when the budget record was updated
 * @param active      flag indicating whether the budget is currently active ({@code true})
 * or soft-deleted ({@code false})
 */
@Builder
public record BudgetResponse(
        UUID id,
        UUID categoryId,
        BigDecimal amountLimit,
        Integer month,
        Integer year,
        Instant createdAt,
        Instant updatedAt,
        boolean active
) {
}
