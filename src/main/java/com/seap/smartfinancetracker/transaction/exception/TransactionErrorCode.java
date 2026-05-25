package com.seap.smartfinancetracker.transaction.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the Transaction module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum TransactionErrorCode implements ErrorCode {

    /**
     * Error indicating that the requested transaction could not be found.
     * <p>
     * Thrown when a transaction ID does not exist in the database, or if the
     * authenticated user attempts to access/modify a transaction they do not own.
     * </p>
     */
    TRANSACTION_NOT_FOUND("TRA-404-01", "Transaction Not Found", HttpStatus.NOT_FOUND.value()),

    /**
     * Error indicating a duplicate transaction attempt.
     * <p>
     * Thrown when a request contains an idempotency key that has already been processed.
     * This strictly prevents accidental double-charges or duplicate financial records
     * caused by network retries or double-clicks from the client application.
     * </p>
     */
    IDEMPOTENCY_KEY_EXISTS("TRA-409-01",
            "A transaction with this idempotency key already exists",
            HttpStatus.CONFLICT.value()),

    /**
     * Error indicating a mismatch between the requested transaction type and its category.
     * <p>
     * Thrown during creation if a user attempts to map an {@code INCOME} transaction
     * to an {@code EXPENSE} category (or vice versa), ensuring data integrity.
     * </p>
     */
    TRANSACTION_TYPE_CONFLICT("TRA-409-02",
            "Transaction type does not match the category's defined type",
            HttpStatus.CONFLICT.value()),

    /**
     * Error indicating insufficient funds to complete the transaction.
     * <p>
     * Thrown when a new expense transaction would cause the user's overall balance
     * to drop below the allowed minimum threshold (overdraft limit).
     * </p>
     */
    OVERDRAFT_LIMIT_EXCEEDED("TRA-400-01",
            "Transaction refused. Please reduce the expense or add income to proceed.",
            HttpStatus.BAD_REQUEST.value()),

    /**
     * Error indicating an illegal update operation on a transaction's underlying type.
     * <p>
     * Thrown when updating a transaction if the user attempts to swap the current category
     * for one of an opposing type (e.g., converting an existing {@code EXPENSE} record into
     * an {@code INCOME} record). To change types, the user must delete the old transaction
     * and create a new one.
     * </p>
     */
    TRANSACTION_TYPE_CANNOT_CHANGE("TRA-400-02",
            "Cannot change category to a different transaction type!",
            HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
