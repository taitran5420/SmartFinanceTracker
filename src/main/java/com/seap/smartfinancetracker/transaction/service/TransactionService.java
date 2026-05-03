package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionFilterRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.TransactionUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest);

    TransactionResponse getTransactionById(UUID userId, UUID transactionId);

    Slice<TransactionResponse> getTransactions(UUID userId, TransactionFilterRequest transactionFilterRequest, Pageable pageable);

    TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest transactionUpdateRequest);

    void deleteTransaction(UUID userId, UUID transactionId);
}
