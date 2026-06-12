package com.seap.smartfinancetracker.category.constant;

/**
 * Utility class containing validation error messages for the Category domain.
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class CategoryValidationMessage {
    private CategoryValidationMessage() {}

    public static final String CATEGORY_NAME_CANNOT_BE_BLANK = "Category name cannot be blank";

    public static final String TRANSACTION_TYPE_IS_REQUIRED = "Transaction type is required";
}
