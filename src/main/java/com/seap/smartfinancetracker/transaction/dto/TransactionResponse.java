package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record TransactionResponse(
        UUID id,
        UUID categoryId,
        BigDecimal amount,
        TransactionType transactionType,
        String note,
        Instant createdAt,
        boolean overBudget,
        boolean active
) { }
