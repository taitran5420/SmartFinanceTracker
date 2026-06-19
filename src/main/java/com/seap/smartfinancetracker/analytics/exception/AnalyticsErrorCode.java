package com.seap.smartfinancetracker.analytics.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the Analytics module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum AnalyticsErrorCode implements ErrorCode {

    /**
     * Error indicating that the supplied analytics window is invalid.
     * <p>
     * Thrown when both date bounds are provided but {@code startDate} is chronologically
     * after {@code endDate}, which can never match any transactions.
     * </p>
     */
    INVALID_DATE_RANGE("ANL-400-01",
            "startDate must not be after endDate",
            HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
