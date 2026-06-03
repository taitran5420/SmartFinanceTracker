package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;

import java.util.List;
import java.util.UUID;

public interface RecurringTransactionService {

    RecurringTransactionResponse createRecurring(UUID userId, RecurringTransactionCreateRequest request);

    RecurringTransactionResponse getRecurringById(UUID userId, UUID id);

    RecurringTransactionResponse updateRecurring(UUID userId, UUID id, RecurringTransactionUpdateRequest request);

    void deleteRecurring(UUID userId, UUID id);

    RecurringTransactionResponse toggleActiveStatus(UUID userId, UUID id);

    List<UpcomingRecurringResponse> getUpcomingTransactions(UUID userId);

    void processDueRecurringTransactions();
}