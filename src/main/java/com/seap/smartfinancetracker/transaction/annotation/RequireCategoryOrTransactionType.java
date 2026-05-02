package com.seap.smartfinancetracker.transaction.annotation;

import com.seap.smartfinancetracker.transaction.validation.RequireCategoryOrTransactionTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequireCategoryOrTransactionTypeValidator.class)
public @interface RequireCategoryOrTransactionType {
    String message() default "Either categoryId or transactionType must be provided!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
