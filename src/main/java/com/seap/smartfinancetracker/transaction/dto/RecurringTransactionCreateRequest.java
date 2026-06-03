package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.constant.TransactionValidationMessage;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record RecurringTransactionCreateRequest(
        @NotNull(message = TransactionValidationMessage.CATEGORY_ID_IS_REQUIRED)
        UUID categoryId,

        @NotNull(message = TransactionValidationMessage.AMOUNT_IS_REQUIRED)
        @Positive(message = TransactionValidationMessage.AMOUNT_MUST_BE_POSITIVE)
        BigDecimal amount,

        String note,

        @NotNull(message = TransactionValidationMessage.FREQUENCY_IS_REQUIRED)
        Frequency frequency,

        @NotNull(message = TransactionValidationMessage.START_DATE_IS_REQUIRED)
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = TransactionValidationMessage.EXECUTION_TIME_IS_REQUIRED)
        LocalTime executionTime
) {
}
