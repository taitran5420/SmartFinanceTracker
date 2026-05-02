package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.annotation.RequireCategoryOrTransactionType;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@RequireCategoryOrTransactionType
public record TransactionCreateRequest(
        UUID categoryId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be strictly positive")
        BigDecimal amount,

        TransactionType transactionType,

        String note,
        UUID idempotencyKey
) {
}
