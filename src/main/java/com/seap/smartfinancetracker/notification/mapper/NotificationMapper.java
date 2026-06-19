package com.seap.smartfinancetracker.notification.mapper;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import com.seap.smartfinancetracker.notification.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * Component responsible for mapping internal {@link Notification} entities
 * to their corresponding Data Transfer Objects (DTOs).
 * <p>
 * This mapper strictly enforces the boundary between the application's persistence layer
 * and its presentation/transport layer. It transforms raw database records into sanitized
 * {@link NotificationResponse} payloads that are safe for client consumption via standard
 * REST APIs or real-time Server-Sent Events (SSE) streams.
 * </p>
 */
@Component
public class NotificationMapper {

    /**
     * Converts a persisted {@link Notification} entity into a {@link NotificationResponse} DTO.
     * <p>
     * <b>Safety Note:</b> This method includes an explicit null-check to prevent
     * {@link NullPointerException}s when processing potentially missing or uninitialized data.
     * </p>
     *
     * @param notification the core notification entity retrieved from the database
     * @return a fully populated {@link NotificationResponse} ready for client delivery,
     * or {@code null} if the input entity is null
     */
    public NotificationResponse toNotificationResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .isRead(notification.isRead())
                .build();
    }
}
