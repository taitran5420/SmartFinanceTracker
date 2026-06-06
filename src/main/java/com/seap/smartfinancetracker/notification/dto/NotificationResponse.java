package com.seap.smartfinancetracker.notification.dto;

import com.seap.smartfinancetracker.notification.enums.NotificationType;
import lombok.Builder;

import java.util.UUID;

/**
 * Data Transfer Object representing a user notification.
 * <p>
 * <b>Implementation Note:</b> Uses {@code @Builder(toBuilder = true)} to allow easy cloning
 * and state modification.
 * </p>
 *
 * @param id               the unique identifier of the notification
 * @param title            the brief headline or subject of the notification
 * @param message          the detailed body content of the notification
 * @param notificationType the category/type of the notification (e.g., BUDGET_ALERT, SYSTEM_UPDATE)
 * @param isRead           flag indicating whether the user has viewed this notification ({@code true}) or not ({@code false})
 */
@Builder(toBuilder = true)
public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType notificationType,
        boolean isRead
) {
}
