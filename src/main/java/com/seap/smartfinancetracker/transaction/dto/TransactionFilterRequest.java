package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the criteria for filtering transactions.
 * <p>
 * This record is typically used to bind HTTP query parameters for fetching
 * a specific subset of the user's transactions. All fields are optional.
 * When multiple criteria are provided, they are typically combined using
 * logical AND operations.
 * </p>
 *
 * @param startDate       the inclusive start timestamp for the date range filter
 * @param endDate         the inclusive end timestamp for the date range filter
 * @param categoryId      the unique identifier to filter transactions by a specific category
 * @param transactionType the classification to filter by (e.g., INCOME or EXPENSE)
 */
public record TransactionFilterRequest(
        Instant startDate,
        Instant endDate,
        UUID categoryId,
        TransactionType transactionType
) {
}
