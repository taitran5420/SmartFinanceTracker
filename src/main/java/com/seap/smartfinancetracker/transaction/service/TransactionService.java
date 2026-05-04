package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest);

    TransactionResponse getTransactionById(UUID userId, UUID transactionId);

    Slice<TransactionResponse> getTransactions(UUID userId, TransactionFilterRequest transactionFilterRequest, Pageable pageable);

    TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest transactionUpdateRequest);

    void deleteTransaction(UUID userId, UUID transactionId);

    BalanceResponse getBalanceByUserId(UUID userId);
}
