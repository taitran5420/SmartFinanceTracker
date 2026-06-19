package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.Frequency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a lightweight projection of an upcoming scheduled transaction.
 *
 * @param id                 the unique identifier of the recurring configuration
 * @param categoryName       the target category name for display purposes
 * @param amount             the scheduled monetary value
 * @param frequency          the execution interval
 * @param nextOccurrenceDate the exact calendar date of the next automated execution
 * @param executionTime      the exact time of day for the execution
 * @param daysUntilDue       the pre-calculated number of days remaining until execution
 */
@Builder
public record UpcomingRecurringResponse(
        UUID id,
        String categoryName,
        BigDecimal amount,
        Frequency frequency,
        LocalDate nextOccurrenceDate,
        LocalTime executionTime,
        long daysUntilDue
) {
}
