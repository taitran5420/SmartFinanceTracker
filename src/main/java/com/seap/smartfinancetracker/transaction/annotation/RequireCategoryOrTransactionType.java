package com.seap.smartfinancetracker.transaction.annotation;

import com.seap.smartfinancetracker.transaction.constant.TransactionValidationMessage;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.validation.RequireCategoryOrTransactionTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom class-level validation constraint to ensure that an object contains
 * either a {@code categoryId} or a {@code transactionType}.
 * <p>
 * This annotation is typically applied to {@link TransactionCreateRequest}. It enforces the business rule that a
 * transaction must be identifiable by at least one of these two attributes.
 * </p>
 *
 * @see RequireCategoryOrTransactionTypeValidator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequireCategoryOrTransactionTypeValidator.class)
public @interface RequireCategoryOrTransactionType {
    String message() default TransactionValidationMessage.EITHER_CATEGORY_OR_TRANSACTION_TYPE_IS_REQUIRED;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
