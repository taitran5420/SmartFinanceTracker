package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class RecurringTransactionMapper {
    public UpcomingRecurringResponse toUpcomingRecurringResponse(RecurringTransaction recurringTransaction) {
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
        return TransactionCreateRequest.builder()
                .categoryId(recurringTransaction.getCategory().getId())
                .amount(recurringTransaction.getAmount())
                .transactionType(recurringTransaction.getTransactionType())
                .note(recurringTransaction.getNote())
                .idempotencyKey(UUID.randomUUID())
                .build();
    }
}
