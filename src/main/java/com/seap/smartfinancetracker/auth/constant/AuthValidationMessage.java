package com.seap.smartfinancetracker.auth.constant;

/**
 * Utility class containing validation error messages for the Auth domain.
 * <p>
 * This class cannot be instantiated.
 * </p>
 */
public final class AuthValidationMessage {

    private AuthValidationMessage() {}

    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Email format is invalid";
    public static final String EMAIL_MAX_LENGTH = "Email must not exceed 100 characters";

    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_LENGTH = "Password must be between 8 and 50 characters";

    public static final String FULL_NAME_REQUIRED = "Full name is required";
    public static final String FULL_NAME_LENGTH = "Full name must be between 2 and 100 characters";
}
