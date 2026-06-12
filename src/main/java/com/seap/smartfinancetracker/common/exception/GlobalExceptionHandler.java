package com.seap.smartfinancetracker.common.exception;

import com.seap.smartfinancetracker.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Handles application-wide exceptions and converts them
 * into standardized API error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_FAILED";
    private static final String BAD_REQUEST_ERROR_CODE = "BAD_REQUEST";
    private static final String UNAUTHORIZED_ERROR_CODE = "UNAUTHORIZED_ERROR";
    private static final String FORBIDDEN_ERROR_CODE = "FORBIDDEN_ERROR";
    private static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private static final String FORBIDDEN_ERROR_MESSAGE = "You do not have permission to access this resource";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse errorResponse = new ErrorResponse(
                errorCode.getHttpStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );

        log.warn("Business Exception [{}]: {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST_ERROR_CODE,
                ex.getMessage()
        );

        log.warn("Bad Request - IllegalArgumentException: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation Error: {}", errorMessage);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                VALIDATION_ERROR_CODE,
                errorMessage
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication Failed: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                UNAUTHORIZED_ERROR_CODE,
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access Denied: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                FORBIDDEN_ERROR_CODE,
                FORBIDDEN_ERROR_MESSAGE
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unhandled Exception caught in GlobalExceptionHandler:", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                INTERNAL_SERVER_ERROR_CODE,
                INTERNAL_SERVER_ERROR_MESSAGE
        );


        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
