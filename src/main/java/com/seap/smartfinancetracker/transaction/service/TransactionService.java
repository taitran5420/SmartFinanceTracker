package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

/**
 * Service interface defining the business logic contract for managing financial transactions.
 * <p>
 * This service provides operations for creating, retrieving, updating, and deleting
 * transactions, as well as calculating user balances. Implementations of this interface
 * are expected to enforce core business rules, ensure data isolation (tenant-like scoping
 * by user ID), and handle resource-not-found scenarios gracefully.
 * </p>
 */
public interface TransactionService {

    /**
     * Creates a new financial transaction for the specified user.
     *
     * @param userId                   the unique identifier of the user creating the transaction
     * @param transactionCreateRequest the payload containing transaction details
     * @return a {@link TransactionResponse} representing the newly created transaction
     */
    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest);

    /**
     * Retrieves a specific transaction by its unique identifier, ensuring it belongs to the user.
     *
     * @param userId        the unique identifier of the user requesting the transaction
     * @param transactionId the unique identifier of the transaction to retrieve
     * @return a {@link TransactionResponse} representing the requested transaction
     */
    TransactionResponse getTransactionById(UUID userId, UUID transactionId);

    /**
     * Retrieves a paginated slice of transactions for the specified user, optionally filtered.
     * <p>
     * <b>Performance Note:</b> A {@link Slice} is returned instead of a full Page to optimize
     * database performance by avoiding the heavy {@code COUNT(*)} query. This is ideal for
     * "infinite scroll" UI implementations.
     * </p>
     *
     * @param userId                   the unique identifier of the user
     * @param transactionFilterRequest optional filtering criteria (e.g., date range, category)
     * @param pageable                 pagination and sorting information
     * @return a {@link Slice} of {@link TransactionResponse} objects matching the criteria
     */
    Slice<TransactionResponse> getTransactions(UUID userId, TransactionFilterRequest transactionFilterRequest, Pageable pageable);

    /**
     * Updates an existing transaction for the specified user.
     * <p>
     * Note that core attributes like transaction type are immutable. Only specific
     * mutable fields provided in the request payload will be updated.
     * </p>
     *
     * @param userId                   the unique identifier of the user requesting the update
     * @param transactionId            the unique identifier of the transaction to update
     * @param transactionUpdateRequest the payload containing the updated transaction details
     * @return a {@link TransactionResponse} representing the updated transaction
     */
    TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest transactionUpdateRequest);

    /**
     * Deletes a specific transaction belonging to the specified user.
     * <p>
     * Implementations should perform a soft delete to maintain data integrity and
     * accurate historical balance calculations.
     * </p>
     *
     * @param userId        the unique identifier of the user requesting the deletion
     * @param transactionId the unique identifier of the transaction to delete
     */
    void deleteTransaction(UUID userId, UUID transactionId);

    /**
     * Calculates the current financial balance for the specified user.
     * <p>
     * The balance is derived by aggregating the total income and subtracting the total expenses
     * across all active transactions.
     * </p>
     *
     * @param userId the unique identifier of the user
     * @return a {@link BalanceResponse} containing the aggregated income, expense, and net balance
     */
    BalanceResponse getBalanceByUserId(UUID userId);
}
