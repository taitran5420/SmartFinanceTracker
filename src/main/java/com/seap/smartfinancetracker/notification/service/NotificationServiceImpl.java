package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.entity.Notification;
import com.seap.smartfinancetracker.notification.enums.NotificationType;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.notification.mapper.NotificationMapper;
import com.seap.smartfinancetracker.notification.repository.NotificationRepository;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseNotificationService sseNotificationService;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void createOverdraftNotification(UUID userId, OverdraftAlertEvent overdraftAlertEvent) {
        User userRef = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(userRef)
                .title("Scheduled Transaction Failed")
                .message(String.format("Could not process transaction for '%s'. Reason: %s",
                        overdraftAlertEvent.categoryName(),
                        overdraftAlertEvent.errorMessage()))
                .notificationType(NotificationType.OVERDRAFT_ALERT)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        sseNotificationService.pushNotificationToUser(userId, notificationMapper.toNotificationResponse(saved));
    }

    @Override
    @Transactional
    public void createTransactionSuccessNotification(UUID userId, TransactionCreatedEvent transactionCreatedEvent) {
        User userRef = userRepository.getReferenceById(userId);
        String action = transactionCreatedEvent.transactionType().equals(TransactionType.EXPENSE) ? "spent" : "received";

        Notification notification = Notification.builder()
                .user(userRef)
                .title("Transaction Successful")
                .message(String.format("Successfully %s %s for '%s'.", action,
                        transactionCreatedEvent.amount(),
                        transactionCreatedEvent.categoryName()))
                .notificationType(NotificationType.TRANSACTION_SUCCESS)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        sseNotificationService.pushNotificationToUser(userId, notificationMapper.toNotificationResponse(saved));
    }
}
