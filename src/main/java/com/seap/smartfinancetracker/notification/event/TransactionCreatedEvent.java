package com.seap.smartfinancetracker.notification.event;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event emitted immediately following the successful persistence of a new transaction.
 *
 * @param userId          the unique identifier of the user who owns the new transaction
 * @param categoryName    the denormalized name of the category, provided for immediate use in notification templates
 * @param amount          the monetary value of the executed transaction
 * @param transactionType the classification of the transaction (e.g., INCOME, EXPENSE)
 */
@Builder
public record TransactionCreatedEvent (
        UUID userId,
        String categoryName,
        BigDecimal amount,
        TransactionType transactionType
) {
}
