package com.seap.smartfinancetracker.transaction.processor;

import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.dto.OverdraftAlertEvent;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.mapper.RecurringTransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionProcessor {
    private final TransactionService transactionService;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRecurringTransaction(RecurringTransaction recurringTransaction) {
        UUID userId = recurringTransaction.getUser().getId();
        try {
            TransactionCreateRequest transactionCreateRequest = recurringTransactionMapper
                    .toTransactionCreateRequest(recurringTransaction);

            transactionService.createTransaction(userId, transactionCreateRequest);
            log.info("Auto-executed transaction {} for user {}", recurringTransaction.getId(), userId);
        } catch (BusinessException e) {
            log.warn("Skipped transaction {} due to rule violation: {}", recurringTransaction.getId(), e.getMessage());

            eventPublisher.publishEvent(new OverdraftAlertEvent(
                    userId,
                    recurringTransaction.getCategory().getCategoryName(),
                    e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error executing recurring transaction {}", recurringTransaction.getId(), e);
        }

        updateTransactionLifecycle(recurringTransaction);
    }

    private void updateTransactionLifecycle(RecurringTransaction recurringTransaction) {
        RecurringTransaction.RecurringTransactionBuilder builder = recurringTransaction.toBuilder();

        if (recurringTransaction.getFrequency() == Frequency.ONCE) {
            builder.active(false);
            log.info("Scheduled ONE-TIME transaction {} completed and is now inactive", recurringTransaction.getId());
        }
        else {
            LocalDate nextDate = calculateNextOccurrence(recurringTransaction.getNextOccurrenceDate(), recurringTransaction.getFrequency());
            builder.nextOccurrenceDate(nextDate);

            if (recurringTransaction.getEndDate() != null && nextDate.isAfter(recurringTransaction.getEndDate())) {
                builder.active(false);
                log.info("Recurring transaction {} reached its end date and is now inactive", recurringTransaction.getId());
            }
        }

        recurringTransactionRepository.save(builder.build());
    }

    private LocalDate calculateNextOccurrence(LocalDate current, Frequency frequency) {
        return switch (frequency) {
            case ONCE -> current;
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }
}
