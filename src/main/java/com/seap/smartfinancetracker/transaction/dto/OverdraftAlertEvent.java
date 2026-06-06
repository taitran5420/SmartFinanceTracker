package com.seap.smartfinancetracker.transaction.dto;

import lombok.Builder;

import java.util.UUID;

/**
 * Domain event payload triggered when an automated transaction fails due to insufficient funds.
 *
 * @param userId       the unique identifier of the user who owns the transaction
 * @param categoryName the target category of the failed transaction
 * @param errorMessage the exact reason for the failure (e.g., "Exceeds overdraft limit")
 */
@Builder
public record OverdraftAlertEvent(
        UUID userId,
        String categoryName,
        String errorMessage
) {
}
