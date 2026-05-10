package com.seap.smartfinancetracker.transaction.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing the user's financial balance summary.
 * <p>
 * This record encapsulates the total aggregated income and expenses,
 * along with the calculated net current balance.
 * </p>
 *
 * @param totalIncome the sum of all income transactions
 * @param totalExpense the sum of all expense transactions
 * @param currentBalance the net balance (typically total income minus total expense)
 */
@Builder
public record BalanceResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal currentBalance
) { }