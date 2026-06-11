package com.seap.smartfinancetracker.transaction.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of per-category expense spending, enriched with the category name so
 * callers need not perform a separate lookup to resolve names.
 * <p>
 * Produced by {@link TransactionRepository#findCategorySpending}.
 * </p>
 */
public interface CategorySpendingProjection {

    /**
     * @return the unique identifier of the category
     */
    UUID getCategoryId();

    /**
     * @return the human-readable name of the category
     */
    String getCategoryName();

    /**
     * @return the total expense amount recorded against this category for the period
     */
    BigDecimal getTotalSpent();
}
