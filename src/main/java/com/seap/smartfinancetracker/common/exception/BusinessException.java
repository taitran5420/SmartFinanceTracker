package com.seap.smartfinancetracker.common.exception;

import lombok.Getter;

/**
 * The base unchecked exception for all custom business logic errors within the application.
 * <p>
 * This exception serves as a generic wrapper for any {@link ErrorCode} implementation
 * (e.g., CategoryErrorCode, BudgetErrorCode). Throwing this exception triggers a rollback
 * in Spring's {@code @Transactional} context and is intended to be caught globally by a
 * {@code @RestControllerAdvice} to return a standardized API error response.
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * Constructs a new BusinessException with the specified error code.
     *
     * @param errorCode the detailed error code enum instance
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
