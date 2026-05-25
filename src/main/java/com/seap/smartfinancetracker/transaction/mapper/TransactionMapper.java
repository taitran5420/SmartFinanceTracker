package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Component responsible for mapping between {@link Transaction} entities
 * and their corresponding Data Transfer Objects (DTOs).
 */
@Component
public class TransactionMapper {

    /**
     * Converts a transaction creation request into a {@link Transaction} entity.
     * <p>
     * <b>Performance Note:</b> This method constructs proxy-like {@link User} and
     * {@link Category} objects containing only their IDs. This is a deliberate
     * optimization allowing JPA/Hibernate to establish foreign key relationships
     * during the {@code INSERT} statement without requiring prior {@code SELECT} queries.
     * </p>
     *
     * @param userId the unique identifier of the user creating the transaction
     * @param transactionCreateRequest the payload containing transaction details
     * @return a new {@link Transaction} entity ready to be persisted,
     *         or {@code null} if the request payload is null
     */
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

    /**
     * Converts a persisted {@link Transaction} entity into a {@link TransactionResponse} DTO.
     * <p>
     * <b>Note:</b> This is a convenience method that delegates to
     * {@link #toResponse(Transaction, String)} with a {@code null} warning message.
     * </p>
     *
     * @param transaction the transaction entity retrieved from the database
     * @return the mapped response DTO, or {@code null} if the entity is null
     * @see #toResponse(Transaction, String)
     */
    public TransactionResponse toResponse(Transaction transaction){
        return toResponse(transaction, null);
    }

    /**
     * Converts a persisted {@link Transaction} entity into a {@link TransactionResponse} DTO,
     * optionally including a warning message.
     *
     * @param transaction    the transaction entity retrieved from the database
     * @param warningMessage an optional warning message to attach to the response
     * (e.g., alerts for exceeding a budget threshold); can be {@code null}
     * @return the mapped response DTO, or {@code null} if the entity is null
     */
    public TransactionResponse toResponse(Transaction transaction, String warningMessage) {
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
                .updatedAt(transaction.getUpdatedAt())
                .isOverBudget(transaction.isOverBudget())
                .warningMessage(warningMessage)
                .build();
    }
}
