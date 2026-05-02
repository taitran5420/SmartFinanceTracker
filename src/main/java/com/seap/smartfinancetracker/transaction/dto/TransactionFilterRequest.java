package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record TransactionFilterRequest(
        Instant startDate,
        Instant endDate,
        UUID categoryId,
        TransactionType transactionType
) {
}
