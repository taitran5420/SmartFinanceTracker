package com.seap.smartfinancetracker.transaction.constant;

/**
 * Utility class containing validation error messages for the Transaction domain.
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class TransactionValidationMessage {
    private TransactionValidationMessage() {}

    public static final String EITHER_CATEGORY_OR_TRANSACTION_TYPE_IS_REQUIRED = "Either categoryId or transactionType must be provided!";

    public static final String AMOUNT_IS_REQUIRED = "Amount is required";
    public static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be strictly positive";
}
