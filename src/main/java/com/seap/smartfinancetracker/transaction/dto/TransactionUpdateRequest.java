package com.seap.smartfinancetracker.transaction.dto;

import com.seap.smartfinancetracker.transaction.constant.TransactionValidationMessage;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for updating an existing financial transaction.
 * <p>
 * This record encapsulates the payload used to modify a transaction's details.
 * Note that core attributes like the transaction type (Income/Expense) are typically
 * immutable after creation; thus, only specific mutable fields are exposed here.
 * </p>
 *
 * @param categoryId the unique identifier of the new category to associate with the transaction
 * @param amount     the updated monetary value of the transaction; if provided, must be strictly positive
 * @param note       the updated note, memo, or description for the transaction
 */
public record TransactionUpdateRequest(
        UUID categoryId,

        @Positive(message = TransactionValidationMessage.AMOUNT_MUST_BE_POSITIVE)
        BigDecimal amount,

        String note
) { }
