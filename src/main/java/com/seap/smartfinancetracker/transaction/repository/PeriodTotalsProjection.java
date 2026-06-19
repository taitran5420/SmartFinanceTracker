package com.seap.smartfinancetracker.transaction.repository;

import java.math.BigDecimal;

/**
 * Read-only projection carrying the three headline figures of a period summary, produced in
 * a single query by {@link TransactionRepository#calculatePeriodTotals}.
 */
public interface PeriodTotalsProjection {

    /**
     * @return the total of all income transactions in the period (never {@code null})
     */
    BigDecimal getTotalIncome();

    /**
     * @return the total of all expense transactions in the period (never {@code null})
     */
    BigDecimal getTotalExpense();

    /**
     * @return the number of active transactions in the period
     */
    long getTransactionCount();
}
