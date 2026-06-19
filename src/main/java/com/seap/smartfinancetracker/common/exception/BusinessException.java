package com.seap.smartfinancetracker.common.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * The base unchecked exception for all custom business logic errors within the application.
 * <p>
 * This exception serves as a generic wrapper for any {@link ErrorCode} implementation
 * (e.g., CategoryErrorCode, BudgetErrorCode). Throwing this exception triggers a rollback
 * in Spring's {@code @Transactional} context and is intended to be caught globally by a
 * {@code @RestControllerAdvice} to return a standardized API error response.
 * </p>
 * <p>
 * An optional {@code context} map can carry extra detail that downstream handlers (e.g. an
 * event-publishing aspect) need but cannot reconstruct from the failed operation's inputs.
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * Additional, never-null detail attached to this exception. Empty unless populated at throw time.
     */
    private final transient Map<String, Object> context;

    /**
     * Constructs a new BusinessException with the specified error code.
     *
     * @param errorCode the detailed error code enum instance
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Collections.emptyMap());
    }

    /**
     * Constructs a new BusinessException with the specified error code and contextual detail.
     *
     * @param errorCode the detailed error code enum instance
     * @param context   extra detail to travel with the exception; a {@code null} value is treated as empty
     */
    public BusinessException(ErrorCode errorCode, Map<String, Object> context) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.context = context == null ? Collections.emptyMap() : Map.copyOf(context);
    }
}
