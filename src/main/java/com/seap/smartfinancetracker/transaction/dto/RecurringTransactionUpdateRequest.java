package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.constant.TransactionValidationMessage;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Data Transfer Object containing the payload for modifying an existing recurring transaction.
 *
 */
@Builder
public record RecurringTransactionUpdateRequest(
        UUID categoryId,

        @Positive(message = TransactionValidationMessage.AMOUNT_MUST_BE_POSITIVE)
        BigDecimal amount,

        String note,

        Frequency frequency,

        LocalDate startDate,

        LocalDate endDate,

        LocalTime executionTime
) {
}
