package com.seap.smartfinancetracker.transaction.controller;

import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for managing financial transactions.
 * <p>
 * This controller provides endpoints for creating, retrieving, updating,
 * and deleting transactions, as well as fetching the user's current balance.
 * </p>
 */
@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    /**
     * Creates a new financial transaction for the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @param transactionCreateRequest the payload containing transaction details
     * @return a {@link ResponseEntity} containing the created {@link TransactionResponse}
     *         with HTTP status 201 (Created)
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @CurrentUserId UUID userId,
            @Valid @RequestBody TransactionCreateRequest transactionCreateRequest
    ) {
            TransactionResponse transactionResponse = transactionService.createTransaction(userId, transactionCreateRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionResponse);
    }

    /**
     * Retrieves a specific transaction by its unique identifier.
     *
     * @param userId the ID of the currently authenticated user
     * @param transactionId the unique identifier of the transaction to retrieve
     * @return a {@link ResponseEntity} containing the requested {@link TransactionResponse}
     * with HTTP status 200 (OK)
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId) {
        TransactionResponse transactionResponse = transactionService.getTransactionById(userId, transactionId);
        return ResponseEntity.ok(transactionResponse);
    }

    /**
     * Retrieves a paginated slice of transactions for the authenticated user,
     * with optional filtering.
     *
     * @param userId the ID of the currently authenticated user
     * @param transactionFilterRequest optional filtering criteria
     * @param pageable pagination and sorting information (defaults to 20 items per page,
     *                 sorted by creation date in descending order)
     * @return a {@link ResponseEntity} containing a {@link Slice} of {@link TransactionResponse}
     *         with HTTP status 200 (OK)
     */
    @GetMapping
    public ResponseEntity<Slice<TransactionResponse>> getAllTransactions(
            @CurrentUserId UUID userId,
            @ModelAttribute TransactionFilterRequest transactionFilterRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Slice<TransactionResponse> transactionResponses = transactionService.getTransactions(
                userId,
                transactionFilterRequest,
                pageable);

        return ResponseEntity.ok(transactionResponses);
    }

    /**
     * Updates an existing transaction for the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @param transactionId the unique identifier of the transaction to update
     * @param transactionUpdateRequest the payload containing the updated transaction details
     * @return a {@link ResponseEntity} containing the updated {@link TransactionResponse}
     *         with HTTP status 200 (OK)
     */
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId,
            @Valid @RequestBody TransactionUpdateRequest transactionUpdateRequest
    ) {
        TransactionResponse transactionUpdatedResponse = transactionService.updateTransaction(userId, transactionId, transactionUpdateRequest);

        return ResponseEntity.ok(transactionUpdatedResponse);
    }

    /**
     * Deletes a specific transaction belonging to the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @param transactionId the unique identifier of the transaction to delete
     * @return an empty {@link ResponseEntity} with HTTP status 204 (No Content)
     */
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(
            @CurrentUserId UUID userId,
            @PathVariable UUID transactionId
    ) {
        transactionService.deleteTransaction(userId, transactionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the current financial balance for the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @return a {@link ResponseEntity} containing the {@link BalanceResponse}
     *         with HTTP status 200 (OK)
     */
    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@CurrentUserId  UUID userId) {
        BalanceResponse balanceResponse =  transactionService.getBalanceByUserId(userId);
        return ResponseEntity.ok(balanceResponse);
    }
}
