package com.seap.smartfinancetracker.notification.event;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent (
        UUID userId,
        String categoryName,
        BigDecimal amount,
        TransactionType transactionType
) {
}
