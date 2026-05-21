package com.seap.smartfinancetracker.transaction.dto;

import java.util.UUID;

public record OverdraftAlertEvent(
        UUID userId,
        String categoryName,
        String errorMessage
) {
}
