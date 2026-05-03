package com.seap.smartfinancetracker.transaction.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionUpdateRequest(
        UUID categoryId,

        @Positive(message = "Amount must be strictly positive")
        BigDecimal amount,

        String note
) { }
