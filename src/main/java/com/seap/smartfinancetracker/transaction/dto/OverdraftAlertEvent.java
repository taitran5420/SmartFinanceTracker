package com.seap.smartfinancetracker.transaction.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record OverdraftAlertEvent(
        UUID userId,
        String categoryName,
        String errorMessage
) {
}
