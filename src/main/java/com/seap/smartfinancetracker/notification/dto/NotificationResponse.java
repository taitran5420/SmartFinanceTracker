package com.seap.smartfinancetracker.notification.dto;

import com.seap.smartfinancetracker.notification.enums.NotificationType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType notificationType,
        boolean isRead
) {
}
