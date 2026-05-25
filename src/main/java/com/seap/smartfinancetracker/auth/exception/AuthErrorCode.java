package com.seap.smartfinancetracker.auth.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the Authentication and Authorization module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling during user registration, login, and identity verification processes.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    /**
     * Error indicating that the authentication request contains missing or invalid fields.
     * <p>
     * Typically thrown during user registration or login when the payload fails
     * standard validation constraints (e.g., malformed email format, weak password, or empty fields).
     * </p>
     */
    INVALID_INPUT("AUTH-400-01", "Missing or invalid required fields", HttpStatus.BAD_REQUEST.value()),

    /**
     * Error indicating that the provided authentication credentials do not match any active user.
     * <p>
     * Thrown during the login process when the email is not found or the password
     * verification fails. For security reasons, the exact cause (wrong email vs. wrong password)
     * is kept intentionally vague to prevent user enumeration attacks.
     * </p>
     */
    INVALID_CREDENTIALS("AUTH-401-01", "Invalid email or password", HttpStatus.UNAUTHORIZED.value()),

    /**
     * Error indicating a conflict during user registration due to a duplicate email address.
     * <p>
     * Thrown to enforce the database uniqueness constraint, ensuring that each email
     * belongs to exactly one user account in the system.
     * </p>
     */
    EMAIL_ALREADY_EXISTS("AUTH-409-01", "Email already exists", HttpStatus.CONFLICT.value()),;

    private final String code;
    private final String message;
    private final int httpStatus;
}
