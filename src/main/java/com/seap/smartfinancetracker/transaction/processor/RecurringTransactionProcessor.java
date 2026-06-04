package com.seap.smartfinancetracker.transaction.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.common.config.ThreadPoolConfig;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.kafka.constant.KafkaConstant;
import com.seap.smartfinancetracker.transaction.dto.OverdraftAlertEvent;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.exception.TransactionErrorCode;
import com.seap.smartfinancetracker.transaction.mapper.RecurringTransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionProcessor {
    private final TransactionService transactionService;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    @Lazy
    private RecurringTransactionProcessor self;

    @Async(ThreadPoolConfig.RECURRING_TASK_EXECUTOR_BEAN_NAME)
    public void processRecurringTransactionForUser(UUID userId, List<RecurringTransaction> recurringTransactions) {
        log.info("Thread {} is processing {} transactions sequentially for user: {}",
                Thread.currentThread().getName(), recurringTransactions.size(), userId);
        recurringTransactions.forEach((recurringTransaction) ->
                self.processSingleRecurringTransaction(userId, recurringTransaction));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRecurringTransaction(UUID userId, RecurringTransaction recurringTransaction) {
        try {
            TransactionCreateRequest transactionCreateRequest = recurringTransactionMapper
                    .toTransactionCreateRequest(recurringTransaction);

            transactionService.createTransaction(userId, transactionCreateRequest);
            log.info("Auto-executed transaction {} for user {}", recurringTransaction.getId(), userId);
        } catch (BusinessException e) {
            log.warn("Skipped transaction {} due to rule violation: {}", recurringTransaction.getId(), e.getMessage());

            if (e.getErrorCode() == TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED) {
                OverdraftAlertEvent overdraftAlertEvent = OverdraftAlertEvent.builder()
                        .userId(userId)
                        .errorMessage(e.getMessage())
                        .categoryName(recurringTransaction.getCategory().getCategoryName())
                        .build();
                try {
                    String overdraftJsonPayload = objectMapper.writeValueAsString(overdraftAlertEvent);

                    kafkaTemplate.send(KafkaConstant.OVERDRAFT_ALERT_TOPIC, overdraftJsonPayload);
                } catch (JsonProcessingException ex) {
                    log.error("Failed to convert event to JSON for user: {}", userId, ex);
                }
            }
        }  catch (Exception e) {
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
