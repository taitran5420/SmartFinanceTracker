package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    private final TransactionMapper transactionMapper;


    @Override
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.idempotencyKey() != null && transactionRepository.existsByIdempotencyKey(transactionCreateRequest.idempotencyKey())) {
            log.warn("Duplicate transaction attempt detected with idempotency key: {}", transactionCreateRequest.idempotencyKey());
            throw new IllegalArgumentException("A transaction with this idempotency key already exists!");
        }
        Category category;
        TransactionType finalTransactionType;

        if (transactionCreateRequest.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(transactionCreateRequest.categoryId(), userId).orElse(null);
            if (category == null) {
                category = categoryRepository.findByIdAndUserIdIsNull(transactionCreateRequest.categoryId()).orElse(null);
            }

            if (category == null) {
                throw new IllegalArgumentException("Category Not Found!");
            }
            // TODO
        }

        return null;
    }

    @Override
    public TransactionResponse getTransactionById(UUID userId, UUID transactionId) {
        // TODO
        return null;
    }

    @Override
    public Slice<TransactionResponse> getTransactions(UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
        // TODO
        return null;
    }
}
