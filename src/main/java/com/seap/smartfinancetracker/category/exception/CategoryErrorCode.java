package com.seap.smartfinancetracker.category.exception;

import com.seap.smartfinancetracker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of error codes specific to the Category module.
 * <p>
 * Implements the {@link ErrorCode} contract to provide consistent error structures
 * for global exception handling.
 * </p>
 */
@Getter
@AllArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    /**
     * Error indicating that the requested category could not be found in the database.
     * <p>
     * This typically occurs when a category ID does not exist, or if the authenticated
     * user attempts to access a category they do not own.
     * </p>
     */
    CATEGORY_NOT_FOUND("CAT-404-01", "Category Not Found", HttpStatus.NOT_FOUND.value()),

    /**
     * Error indicating that an operation was attempted on a soft-deleted category.
     */
    CATEGORY_INACTIVE("CAT-400-01", "Category is inactive", HttpStatus.BAD_REQUEST.value()),

    /**
     * Error indicating that the authenticated user lacks the necessary permissions
     * to perform an operation on the specified category.
     */
    CATEGORY_ACCESS_DENIED("CAT-403-01",
            "You do not have permission to access this category!",
            HttpStatus.FORBIDDEN.value());

    private final String code;
    private final String message;
    private final int httpStatus;
}
