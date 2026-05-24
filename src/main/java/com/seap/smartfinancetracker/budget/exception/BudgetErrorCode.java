package com.seap.smartfinancetracker.budget.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BudgetErrorCode implements ErrorCode {
    BUDGET_NOT_FOUND("BGT-404-01", "Budget Not Found", HttpStatus.NOT_FOUND.value()),

    CATEGORY_MUST_BE_EXPENSE("BGT-400-01",
            "Budgets can only be created for EXPENSE categories",
            HttpStatus.BAD_REQUEST.value()),
    BUDGET_NOT_ACTIVE("BGT-400-02", "Budget Not Active", HttpStatus.BAD_REQUEST.value()),

    ACTIVE_BUDGET_EXISTS("BGT-409-01",
            "An active budget already exists for this period",
            HttpStatus.CONFLICT.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
