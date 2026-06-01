package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;

import java.util.UUID;

public interface NotificationService {
    void createOverdraftNotification(UUID userId, OverdraftAlertEvent overdraftAlertEvent);
    void createTransactionSuccessNotification(UUID userId, TransactionCreatedEvent transactionCreatedEvent);
}
