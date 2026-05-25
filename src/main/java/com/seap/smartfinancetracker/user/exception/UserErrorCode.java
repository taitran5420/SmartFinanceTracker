package com.seap.smartfinancetracker.user.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the User module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling across the application.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    /**
     * Error indicating that the requested user could not be found in the system.
     * <p>
     * Thrown when an operation targets a user ID that does not exist in the database,
     * or when authentication processes fail to resolve the current active user from the security context.
     * </p>
     */
    USER_NOT_FOUND("USR-404-01", "User not found", HttpStatus.NOT_FOUND.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
