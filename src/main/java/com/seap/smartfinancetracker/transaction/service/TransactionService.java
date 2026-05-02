package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest);

    TransactionResponse getTransactionById(UUID userId, UUID transactionId);

    Slice<TransactionResponse> getTransactions(UUID userId, Instant startDate, Instant endDate, Pageable pageable);
}
