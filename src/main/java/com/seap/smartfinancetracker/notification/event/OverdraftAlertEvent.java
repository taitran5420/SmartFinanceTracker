package com.seap.smartfinancetracker.notification.event;

import java.util.UUID;

public record OverdraftAlertEvent(
        UUID userId,
        String categoryName,
        String errorMessage
) {
}
