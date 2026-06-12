package com.seap.smartfinancetracker.category.dto;

import com.seap.smartfinancetracker.category.constant.CategoryValidationMessage;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for creating a category.
 *
 * @param categoryName the new category name
 * @param transactionType the category transaction type
 */
public record CategoryCreateRequest(
    @NotBlank(message = CategoryValidationMessage.CATEGORY_NAME_CANNOT_BE_BLANK)
    String categoryName,

    @NotNull(message = CategoryValidationMessage.TRANSACTION_TYPE_IS_REQUIRED)
    TransactionType transactionType
) { }