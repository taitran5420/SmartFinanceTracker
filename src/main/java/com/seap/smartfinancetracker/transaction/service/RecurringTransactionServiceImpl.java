package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
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
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final RecurringTransactionProcessor recurringTransactionProcessor;

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
