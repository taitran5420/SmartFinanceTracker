package com.seap.smartfinancetracker.category.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record CategoryResponse(
        UUID id,
        String categoryName,
        TransactionType transactionType,
        Instant createdAt,
        boolean active
) { }
