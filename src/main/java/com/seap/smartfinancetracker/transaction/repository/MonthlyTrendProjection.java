package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.enums.TransactionType;

import java.math.BigDecimal;

/**
 * Read-only projection representing a single month/type bucket of aggregated
 * transaction amounts.
 * <p>
 * Produced by {@link TransactionRepository#findMonthlyTrend}, each instance carries the
 * calendar year and month (1&ndash;12) the bucket belongs to, the {@link TransactionType}
 * it aggregates, and the summed amount for that combination.
 * </p>
 */
public interface MonthlyTrendProjection {

    /**
     * @return the four-digit calendar year of this bucket
     */
    Integer getYear();

    /**
     * @return the calendar month of this bucket, in the range 1 (January) to 12 (December)
     */
    Integer getMonth();

    /**
     * @return the transaction classification this bucket aggregates (INCOME or EXPENSE)
     */
    TransactionType getTransactionType();

    /**
     * @return the total amount summed for this year/month/type combination
     */
    BigDecimal getTotalAmount();
}
