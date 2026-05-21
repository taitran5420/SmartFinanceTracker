package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;

import java.util.List;
import java.util.UUID;

public interface RecurringTransactionService {
    List<UpcomingRecurringResponse> getUpcomingTransactions(UUID userId);

    void processDueRecurringTransactions();
}