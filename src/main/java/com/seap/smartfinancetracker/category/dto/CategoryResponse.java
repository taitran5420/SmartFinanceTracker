package com.seap.smartfinancetracker.category.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response object representing category information.
 *
 * @param id the category's id
 * @param categoryName the category's name
 * @param transactionType the category's transaction type
 * @param createdAt the time category was created
 * @param updatedAt the time category was updated
 * @param active return {@code true} if category still active, {@code false} if category already deleted
 */
@Builder
public record CategoryResponse(
        UUID id,
        String categoryName,
        TransactionType transactionType,
        Instant createdAt,
        Instant updatedAt,
        boolean active
) { }
