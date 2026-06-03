package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.exception.CategoryErrorCode;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.exception.RecurringTransactionErrorCode;
import com.seap.smartfinancetracker.transaction.mapper.RecurringTransactionMapper;
import com.seap.smartfinancetracker.transaction.processor.RecurringTransactionProcessor;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final CategoryRepository categoryRepository;

    private final RecurringTransactionMapper recurringTransactionMapper;
    private final RecurringTransactionProcessor recurringTransactionProcessor;

    @Override
    @Transactional
    public RecurringTransactionResponse createRecurring(UUID userId, RecurringTransactionCreateRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        RecurringTransaction recurringTransaction = recurringTransactionMapper.toEntity(userId, request, category);

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toRecurringTransactionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringById(UUID userId, UUID id) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(RecurringTransactionErrorCode.RECURRING_TRANSACTION_NOT_FOUND));

        return recurringTransactionMapper.toRecurringTransactionResponse(recurringTransaction);
    }

    @Override
    public RecurringTransactionResponse updateRecurring(UUID userId, UUID id, RecurringTransactionUpdateRequest request) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(RecurringTransactionErrorCode.RECURRING_TRANSACTION_NOT_FOUND));

        RecurringTransaction.RecurringTransactionBuilder recurringTransactionBuilder = recurringTransaction.toBuilder();

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

            if (category.getTransactionType() != recurringTransaction.getCategory().getTransactionType()) {
                throw new BusinessException(RecurringTransactionErrorCode.UPDATE_CONFLICT_TRANSACTION_TYPE);
            }
            recurringTransactionBuilder.category(category);
        }

        if (request.amount() != null) {
            recurringTransactionBuilder.amount(request.amount());
        }

        if (request.note() != null) {
            recurringTransactionBuilder.note(request.note());
        }

        if (request.frequency() != null) {
            recurringTransactionBuilder.frequency(request.frequency());
        }

        if (request.startDate() != null) {
            recurringTransactionBuilder.startDate(request.startDate());
        }

        if (request.endDate() != null) {
            recurringTransactionBuilder.endDate(request.endDate());
        }

        if (request.executionTime() != null) {
            recurringTransactionBuilder.executionTime(request.executionTime());
        }

        RecurringTransaction updatedRecurringTransaction = recurringTransactionBuilder.build();

        RecurringTransaction savedRecurringTransaction = recurringTransactionRepository.save(updatedRecurringTransaction);
        return recurringTransactionMapper.toRecurringTransactionResponse(savedRecurringTransaction);
    }

    @Override
    @Transactional
    public void deleteRecurring(UUID userId, UUID id) {
        RecurringTransaction existing = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(RecurringTransactionErrorCode.RECURRING_TRANSACTION_NOT_FOUND));

        recurringTransactionRepository.delete(existing);
    }

    @Override
    public RecurringTransactionResponse toggleActiveStatus(UUID userId, UUID id) {
        RecurringTransaction existing = recurringTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(RecurringTransactionErrorCode.RECURRING_TRANSACTION_NOT_FOUND));

        RecurringTransaction updated = existing.toBuilder()
                .active(!existing.isActive())
                .build();

        RecurringTransaction saved = recurringTransactionRepository.save(updated);
        log.info("Toggled active status for recurring transaction {} to {}", id, saved.isActive());
        return recurringTransactionMapper.toRecurringTransactionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingRecurringResponse> getUpcomingTransactions(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate next7Day = today.plusDays(7);

        List<RecurringTransaction> upcomingTransactions = recurringTransactionRepository.findUpComingTransactions(
                userId, today, next7Day
        );

        return upcomingTransactions.stream()
                .map(recurringTransactionMapper::toUpcomingRecurringResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<RecurringTransaction> dueTransactions = recurringTransactionRepository.findDueTransactions(today, now);

        if (!dueTransactions.isEmpty()) {
            log.info("Found {} recurring transactions to process at {} {}", dueTransactions.size(), today, now);
            for (RecurringTransaction recurringTransaction : dueTransactions) {
                recurringTransactionProcessor.processSingleRecurringTransaction(recurringTransaction);
            }
        }
    }
}
