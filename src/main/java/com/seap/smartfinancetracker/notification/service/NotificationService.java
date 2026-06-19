package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business logic contract for system notifications.
 * <p>
 * This service manages the creation, retrieval, and state management of user notifications.
 * It acts as the bridge between background asynchronous events (Kafka) and the real-time
 * presentation layer (SSE/REST).
 * </p>
 */
public interface NotificationService {

    /**
     * Creates and dispatches a notification warning the user of a failed transaction due to overdraft.
     *
     * @param userId              the unique identifier of the user to be notified
     * @param overdraftAlertEvent the domain event containing details about the failed transaction
     */
    void createOverdraftNotification(UUID userId, OverdraftAlertEvent overdraftAlertEvent);

    /**
     * Creates and dispatches a notification confirming a successful financial transaction.
     *
     * @param userId                  the unique identifier of the user to be notified
     * @param transactionCreatedEvent the domain event containing details about the successful transaction
     */
    void createTransactionSuccessNotification(UUID userId, TransactionCreatedEvent transactionCreatedEvent);

    /**
     * Retrieves all unread notifications for a user and automatically marks them as read.
     * <p>
     * This method acts as an atomic operation to fetch the current unread alerts
     * while simultaneously clearing the user's unread badge.
     * </p>
     *
     * @param userId the unique identifier of the target user
     * @return a list of newly read {@link NotificationResponse} objects
     */
    List<NotificationResponse> getUnreadNotifications(UUID userId);
}
