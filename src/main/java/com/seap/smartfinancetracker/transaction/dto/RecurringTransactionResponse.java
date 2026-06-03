package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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
