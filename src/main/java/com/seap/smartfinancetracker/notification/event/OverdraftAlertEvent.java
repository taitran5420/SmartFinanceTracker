package com.seap.smartfinancetracker.notification.event;

import lombok.Builder;

import java.util.UUID;

/**
 * Domain event triggered when a user attempts a transaction that exceeds their allowed limits.
 *
 * @param userId       the unique identifier of the user who triggered the overdraft alert
 * @param categoryName the specific category associated with the failed transaction attempt
 * @param errorMessage the detailed system message explaining the overdraft constraint
 */
@Builder
public record OverdraftAlertEvent(
        UUID userId,
        String categoryName,
        String errorMessage
) {
}
