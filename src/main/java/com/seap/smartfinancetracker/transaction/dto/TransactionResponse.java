package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the details of a financial transaction.
 * <p>
 * This record is returned to the client to provide a comprehensive view
 * of a transaction's state, including its classification, monetary value,
 * and current active status.
 * </p>
 *
 * @param id              the unique identifier of the transaction
 * @param categoryId      the unique identifier of the category associated with this transaction
 * @param amount          the monetary value of the transaction
 * @param transactionType the classification of the transaction (e.g., INCOME or EXPENSE)
 * @param note            any additional notes or descriptions provided for the transaction
 * @param createdAt       the exact timestamp when the transaction was originally recorded
 * @param active          indicates whether the transaction is active ({@code true}) or has been soft-deleted ({@code false})
 */
@Builder
public record TransactionResponse(
        UUID id,
        UUID categoryId,
        BigDecimal amount,
        TransactionType transactionType,
        String note,
        Instant createdAt,
        boolean active
) { }
