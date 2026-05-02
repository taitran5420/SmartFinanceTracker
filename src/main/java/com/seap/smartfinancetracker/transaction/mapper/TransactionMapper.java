package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionMapper {
    public Transaction toEntity(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest == null) {
            return null;
        }

        return Transaction.builder()
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(transactionCreateRequest.categoryId()).build())
                .amount(transactionCreateRequest.amount())
                .transactionType(transactionCreateRequest.transactionType())
                .note(transactionCreateRequest.note())
                .idempotencyKey(transactionCreateRequest.idempotencyKey())
                .active(true)
                .build();
    }

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .categoryId(transaction.getCategory().getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .note(transaction.getNote())
                .active(transaction.isActive())
                .createdAt(transaction.getCreatedAt())
                .overBudget(transaction.isOverBudget())
                .build();
    }
}
