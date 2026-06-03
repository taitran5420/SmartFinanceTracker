package com.seap.smartfinancetracker.transaction.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecurringTransactionErrorCode implements ErrorCode {

    RECURRING_TRANSACTION_NOT_FOUND("RET-404-01",
            "Recurring Transaction Not Found",
            HttpStatus.NOT_FOUND.value()),

    UPDATE_CONFLICT_TRANSACTION_TYPE("RET-409-01",
            "Transaction type cannot be updated",
            HttpStatus.CONFLICT.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
