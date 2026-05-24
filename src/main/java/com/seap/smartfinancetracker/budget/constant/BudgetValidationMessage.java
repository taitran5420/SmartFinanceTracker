package com.seap.smartfinancetracker.budget.constant;

/**
 * Utility class containing validation error messages for the Budget domain.
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class BudgetValidationMessage {
    private BudgetValidationMessage() {}

    public static final String CATEGORY_REQUIRED = "Category cannot be null";

    public static final String AMOUNT_REQUIRED = "Amount is required";
    public static final String AMOUNT_POSITIVE = "Amount must be strictly positive";

    public static final String MONTH_REQUIRED = "Month cannot be null";
    public static final String MONTH_MIN = "Month must be at least 1";
    public static final String MONTH_MAX = "Month must be at most 12";

    public static final String YEAR_REQUIRED = "Year cannot be null";
}
