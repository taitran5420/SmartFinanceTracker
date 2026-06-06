package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Data Transfer Object representing the full state of a recurring transaction configuration.
 * <p>
 * This payload is returned to the client after creation, update, or retrieval operations.
 * It denormalizes essential data (like {@code categoryName} and {@code transactionType})
 * to reduce the number of subsequent API calls required by the frontend to render the UI.
 * </p>
 */
@Builder
public record RecurringTransactionResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        TransactionType transactionType,
        String note,
        Frequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextOccurrenceDate,
        LocalTime executionTime,
        boolean active
) {
}
