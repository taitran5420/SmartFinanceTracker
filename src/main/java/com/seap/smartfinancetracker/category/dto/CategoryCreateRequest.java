package com.seap.smartfinancetracker.category.dto;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryCreateRequest(
    @NotBlank(message = "Category name cannot be blank")
    String categoryName,

    @NotNull(message = "Transaction type is required")
    TransactionType transactionType
) { }