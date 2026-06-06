package com.seap.smartfinancetracker.transaction.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the Recurring Transaction module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling. These codes specifically target failures during
 * the setup, modification, or execution of automated financial schedules.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum RecurringTransactionErrorCode implements ErrorCode {

    /**
     * Error indicating that the requested recurring transaction setup could not be found.
     * <p>
     * Thrown when an operation targets a recurring transaction ID that does not exist,
     * or if the authenticated user attempts to access/modify a schedule they do not own.
     * </p>
     */
    RECURRING_TRANSACTION_NOT_FOUND("RET-404-01",
            "Recurring Transaction Not Found",
            HttpStatus.NOT_FOUND.value()),

    /**
     * Error indicating an illegal attempt to change the fundamental transaction type of a schedule.
     * <p>
     * Thrown when an update request attempts to map an existing recurring schedule to a
     * new category that possesses a different {@code TransactionType} (e.g., trying to change
     * a monthly {@code INCOME} schedule into an {@code EXPENSE} schedule). To achieve this,
     * the user must delete the current schedule and create a new one.
     * </p>
     */
    UPDATE_CONFLICT_TRANSACTION_TYPE("RET-409-01",
            "Transaction type cannot be updated",
            HttpStatus.CONFLICT.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
