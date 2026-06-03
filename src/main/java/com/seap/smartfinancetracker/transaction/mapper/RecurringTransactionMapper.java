package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class RecurringTransactionMapper {

    public RecurringTransactionResponse toRecurringTransactionResponse(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return RecurringTransactionResponse.builder()
                .id(recurringTransaction.getId())
                .categoryId(recurringTransaction.getCategory().getId())
                .categoryName(recurringTransaction.getCategory().getCategoryName())
                .amount(recurringTransaction.getAmount())
                .transactionType(recurringTransaction.getTransactionType())
                .note(recurringTransaction.getNote())
                .frequency(recurringTransaction.getFrequency())
                .startDate(recurringTransaction.getStartDate())
                .endDate(recurringTransaction.getEndDate())
                .nextOccurrenceDate(recurringTransaction.getNextOccurrenceDate())
                .executionTime(recurringTransaction.getExecutionTime())
                .active(recurringTransaction.isActive())
                .build();
    }

    public RecurringTransaction toEntity(UUID userId, RecurringTransactionCreateRequest recurringTransactionCreateRequest, Category category) {
        if (recurringTransactionCreateRequest == null) {
            return null;
        }

        return RecurringTransaction.builder()
                .user(User.builder().id(userId).build())
                .category(category)
                .amount(recurringTransactionCreateRequest.amount())
                .transactionType(category.getTransactionType())
                .note(recurringTransactionCreateRequest.note())
                .frequency(recurringTransactionCreateRequest.frequency())
                .startDate(recurringTransactionCreateRequest.startDate())
                .endDate(recurringTransactionCreateRequest.endDate())
                .executionTime(recurringTransactionCreateRequest.executionTime())
                .active(true)
                .nextOccurrenceDate(recurringTransactionCreateRequest.startDate())
                .build();
    }

    public UpcomingRecurringResponse toUpcomingRecurringResponse(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return UpcomingRecurringResponse.builder()
                .id(recurringTransaction.getId())
                .categoryName(recurringTransaction.getCategory().getCategoryName())
                .amount(recurringTransaction.getAmount())
                .frequency(recurringTransaction.getFrequency())
                .nextOccurrenceDate(recurringTransaction.getNextOccurrenceDate())
                .executionTime(recurringTransaction.getExecutionTime())
                .daysUntilDue(ChronoUnit.DAYS.between(LocalDate.now(), recurringTransaction.getNextOccurrenceDate()))
                .build();
    }

    public TransactionCreateRequest toTransactionCreateRequest(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return TransactionCreateRequest.builder()
                .categoryId(recurringTransaction.getCategory().getId())
                .amount(recurringTransaction.getAmount())
                .transactionType(recurringTransaction.getTransactionType())
                .note(recurringTransaction.getNote())
                .idempotencyKey(UUID.randomUUID())
                .build();
    }
}
