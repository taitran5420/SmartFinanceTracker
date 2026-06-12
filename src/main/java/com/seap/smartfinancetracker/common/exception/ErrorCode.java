package com.seap.smartfinancetracker.common.exception;

/**
 * Standardized interface for defining application-specific error codes.
 */
public interface ErrorCode {
    /**
     * Retrieves the unique, internal business error code.
     * <p>
     * Typically used by frontend clients for programmatic error handling or translation keys
     * with format: {@code {DOMAIN_CODE}_{HTTP_STATUS_CODE}_{INCREMENT_CODE}}.</p>
     * <p>
     *     (e.g., "BGT_400_001")
     * </p>
     *
     * @return the string representation of the error code
     */
    String getCode();

    /**
     * Retrieves the human-readable, descriptive error message.
     * <p>
     * Intended for logging purposes or as a fallback display message for the client.
     * </p>
     *
     * @return the error message
     */
    String getMessage();

    /**
     * Retrieves the appropriate HTTP status code associated with this error.
     *
     * @return the numeric HTTP status code
     */
    int getHttpStatus();
}
