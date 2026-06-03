package com.seap.smartfinancetracker.transaction.constant;

/**
 * Utility class containing validation error messages for the Transaction domain.
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class TransactionValidationMessage {
    private TransactionValidationMessage() {
    }

    public static final String EITHER_CATEGORY_OR_TRANSACTION_TYPE_IS_REQUIRED = "Either categoryId or transactionType must be provided!";

    public static final String AMOUNT_IS_REQUIRED = "Amount is required";
    public static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be strictly positive";

    public static final String CATEGORY_ID_IS_REQUIRED = "Category ID is required";

    public static final String FREQUENCY_IS_REQUIRED = "Frequency is required";

    public static final String START_DATE_IS_REQUIRED = "Start date is required";

    public static final String EXECUTION_TIME_IS_REQUIRED = "Execution time is required";
}
