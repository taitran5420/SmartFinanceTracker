package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.transaction.repository.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    private final TransactionMapper transactionMapper;


    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.idempotencyKey() != null && transactionRepository.existsByIdempotencyKey(transactionCreateRequest.idempotencyKey())) {
            log.warn("Duplicate transaction attempt detected with idempotency key: {}", transactionCreateRequest.idempotencyKey());
            throw new IllegalArgumentException("A transaction with this idempotency key already exists!");
        }

        Category category;
        TransactionType finalTransactionType;

        if (transactionCreateRequest.categoryId() != null) {
            category = categoryRepository.findById(transactionCreateRequest.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

            if (isNotCategoryOwner(userId, category)) {
                log.warn("User {} attempted to use unauthorized category {}", userId, category.getId());
                throw new IllegalArgumentException("You do not have permission to use this category!");
            }

            if (transactionCreateRequest.transactionType() != null && transactionCreateRequest.transactionType() != category.getTransactionType()) {
                log.error("Conflict: Request type {} does not match Category type {}",
                        transactionCreateRequest.transactionType(), category.getTransactionType());
                throw new IllegalArgumentException("Transaction type does not match the category's defined type!");
            }

            finalTransactionType = category.getTransactionType();

        } else {
            finalTransactionType = transactionCreateRequest.transactionType();

            category = categoryRepository.findFirstByUserIdIsNullAndTransactionType(finalTransactionType)
                    .orElseThrow(() -> {
                        log.error("System configuration error: Missing default category for type {}", finalTransactionType);
                        return new IllegalStateException("System Error: Default category for this transaction type is missing!");
                    });
        }

        Transaction transaction = transactionMapper.toEntity(userId, transactionCreateRequest);
        transaction.setCategory(category);
        transaction.setTransactionType(finalTransactionType);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID userId, UUID transactionId) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<TransactionResponse> getTransactions(UUID userId, TransactionFilterRequest transactionFilterRequest, Pageable pageable) {
        Specification<Transaction> specification = TransactionSpecification.getFilterTransaction(userId, transactionFilterRequest);

        Slice<Transaction> transactionSlice = transactionRepository
                .findAll(specification, pageable);

        return transactionSlice.map(transactionMapper::toResponse);
    }

    @Override
    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest transactionUpdateRequest) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        if (transactionUpdateRequest.categoryId() != null && !transactionUpdateRequest.categoryId().equals(transaction.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(transactionUpdateRequest.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

            if (isNotCategoryOwner(userId, newCategory)) {
                throw new IllegalArgumentException("You do not have permission to use this category!");
            }

            if (newCategory.getTransactionType() != transaction.getTransactionType()) {
                throw new IllegalArgumentException("Cannot change category to a different transaction type!");
            }

            transaction.setCategory(newCategory);
        }

        if (transactionUpdateRequest.amount() != null) {
            transaction.setAmount(transactionUpdateRequest.amount());
        }

        if (transactionUpdateRequest.note() != null) {
            transaction.setNote(transactionUpdateRequest.note());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        if (!transaction.isActive()) {
            log.info("Transaction {} is already deactivated", transactionId);
            return;
        }

        transaction.setActive(false);
        transactionRepository.save(transaction);
        log.info("Transaction {} is deactivated", transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByUserId(UUID userId) {
        BigDecimal totalIncome = transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.INCOME);
        BigDecimal totalExpense =  transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.EXPENSE);

        BigDecimal currentBalance = totalIncome.subtract(totalExpense);

        return BalanceResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .currentBalance(currentBalance)
                .build();
    }

    private boolean isNotCategoryOwner(UUID userId, Category category) {
        if (category.getUser() == null) {
            return false;
        }

        return !category.getUser().getId().equals(userId);
    }

    private Transaction getTransactionByUserIdAndTransactionId(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found!"));
    }
}
