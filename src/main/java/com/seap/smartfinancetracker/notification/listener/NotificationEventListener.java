package com.seap.smartfinancetracker.notification.listener;

import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleOverdraftAlertEvent(OverdraftAlertEvent overdraftAlertEvent) {
        log.debug("Async listener caught OverdraftAlertEvent for user: {}", overdraftAlertEvent.userId());
        notificationService.createOverdraftNotification(overdraftAlertEvent.userId(), overdraftAlertEvent);
    }

    @Async
    @EventListener
    public void handleTransactionCreatedEvent(TransactionCreatedEvent transactionCreatedEvent) {
        log.debug("Async listener caught TransactionCreatedEvent for user: {}", transactionCreatedEvent.userId());
        notificationService.createTransactionSuccessNotification(transactionCreatedEvent.userId(), transactionCreatedEvent);
    }
}
