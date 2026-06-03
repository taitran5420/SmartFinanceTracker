package com.seap.smartfinancetracker.notification.event;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record TransactionCreatedEvent (
        UUID userId,
        String categoryName,
        BigDecimal amount,
        TransactionType transactionType
) {
}
