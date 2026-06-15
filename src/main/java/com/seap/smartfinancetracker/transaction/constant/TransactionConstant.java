package com.seap.smartfinancetracker.transaction.constant;

/**
 * Shared, non-message constants for the transaction module.
 */
public final class TransactionConstant {

    private TransactionConstant() {
    }

    /**
     * {@link com.seap.smartfinancetracker.common.exception.BusinessException} context key carrying the
     * resolved category name of an overdraft-rejected transaction, so the event-publishing aspect can
     * build the alert without re-resolving the category.
     */
    public static final String OVERDRAFT_CATEGORY_NAME_KEY = "categoryName";
}
