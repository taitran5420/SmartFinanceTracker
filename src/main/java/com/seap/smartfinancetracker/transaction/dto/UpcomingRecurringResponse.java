package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.Frequency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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
