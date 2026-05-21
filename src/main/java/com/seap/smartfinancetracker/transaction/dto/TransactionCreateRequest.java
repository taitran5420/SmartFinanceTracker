package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.annotation.RequireCategoryOrTransactionType;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for creating a new financial transaction.
 * <p>
 * This record encapsulates the data payload required to log an income or expense.
 * It enforces a class-level validation constraint ensuring that the transaction
 * can be properly classified by providing either a {@code categoryId} or a
 * {@code transactionType}.
 * </p>
 *
 * @param categoryId      the unique identifier of the category associated with the transaction
 * @param amount          the monetary value of the transaction; must be strictly positive
 * @param transactionType the overarching classification of the transaction (e.g., INCOME, EXPENSE)
 * @param note            an optional description, memo, or context for the transaction
 * @param idempotencyKey  an optional unique key provided by the client to safely retry requests
 *                        without accidentally creating duplicate transactions
 */
@RequireCategoryOrTransactionType
@Builder
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
