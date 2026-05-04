package com.seap.smartfinancetracker.transaction.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BalanceResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal currentBalance
) { }