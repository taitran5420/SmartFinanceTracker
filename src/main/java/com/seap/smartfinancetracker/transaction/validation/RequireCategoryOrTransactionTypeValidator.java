package com.seap.smartfinancetracker.transaction.validation;

import com.seap.smartfinancetracker.transaction.annotation.RequireCategoryOrTransactionType;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireCategoryOrTransactionTypeValidator implements ConstraintValidator<RequireCategoryOrTransactionType, TransactionCreateRequest> {
    @Override
    public boolean isValid(TransactionCreateRequest transactionCreateRequest, ConstraintValidatorContext context) {
        if (transactionCreateRequest == null) {
            return true;
        }

        return transactionCreateRequest.categoryId() != null || transactionCreateRequest.transactionType() != null;
    }
}
