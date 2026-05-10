package com.seap.smartfinancetracker.transaction.validation;

import com.seap.smartfinancetracker.transaction.annotation.RequireCategoryOrTransactionType;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the {@link RequireCategoryOrTransactionType} annotation constraint.
 * <p>
 * This class contains the concrete validation logic for a {@link TransactionCreateRequest}.
 * It ensures that the incoming request is valid by checking if at least one of the
 * key identifiers ({@code categoryId} or {@code transactionType}) is provided.
 * </p>
 */
public class RequireCategoryOrTransactionTypeValidator implements ConstraintValidator<RequireCategoryOrTransactionType, TransactionCreateRequest> {

    /**
     * Evaluates whether the provided {@link TransactionCreateRequest} is valid.
     * <p>
     * Note: A {@code null} request is considered valid by default. This is standard
     * validation behavior, leaving null checks to {@code @NotNull} annotations
     * if the object itself is required.
     * </p>
     *
     * @param transactionCreateRequest the request object to validate
     * @param context context in which the constraint is evaluated
     * @return {@code true} if the request is null, or if either {@code categoryId}
     *         or {@code transactionType} is present; {@code false} otherwise.
     */
    @Override
    public boolean isValid(TransactionCreateRequest transactionCreateRequest, ConstraintValidatorContext context) {
        if (transactionCreateRequest == null) {
            return true;
        }

        return transactionCreateRequest.categoryId() != null || transactionCreateRequest.transactionType() != null;
    }
}
