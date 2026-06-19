package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
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

import java.util.List;
import java.util.UUID;

/**
 * Core implementation of the {@link NotificationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TRANSACTION_FAILED_TITLE = "Scheduled Transaction Failed";
    private static final String TRANSACTION_FAILED_MESSAGE = "Could not process transaction for '%s'. Reason: %s";

    private static final String TRANSACTION_SUCCESS_TITLE = "Transaction Successful";
    private static final String TRANSACTION_SUCCESS_MESSAGE = "Successfully %s %s for '%s'.";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseNotificationService sseNotificationService;
    private final NotificationMapper notificationMapper;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>Real-time Dispatch:</b> Immediately after persisting the database record, the generated DTO
     * is pushed to the client via {@link SseNotificationService}.</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public void createOverdraftNotification(UUID userId, OverdraftAlertEvent overdraftAlertEvent) {
        User userRef = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(userRef)
                .title(TRANSACTION_FAILED_TITLE)
                .message(String.format(TRANSACTION_FAILED_MESSAGE,
                        overdraftAlertEvent.categoryName(),
                        overdraftAlertEvent.errorMessage()))
                .notificationType(NotificationType.OVERDRAFT_ALERT)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        sseNotificationService.pushNotificationToUser(userId, notificationMapper.toNotificationResponse(saved));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * Employs the same Proxy optimization and SSE real-time dispatch mechanism as overdraft notifications.
     * Dynamically adjusts the message phrasing ("spent" vs "received") based on the {@link TransactionType}.
     * </p>
     */
    @Override
    @Transactional
    public void createTransactionSuccessNotification(UUID userId, TransactionCreatedEvent transactionCreatedEvent) {
        User userRef = userRepository.getReferenceById(userId);
        String action = transactionCreatedEvent.transactionType().equals(TransactionType.EXPENSE) ? "spent" : "received";

        Notification notification = Notification.builder()
                .user(userRef)
                .title(TRANSACTION_SUCCESS_TITLE)
                .message(String.format(TRANSACTION_SUCCESS_MESSAGE, action,
                        transactionCreatedEvent.amount(),
                        transactionCreatedEvent.categoryName()))
                .notificationType(NotificationType.TRANSACTION_SUCCESS)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        sseNotificationService.pushNotificationToUser(userId, notificationMapper.toNotificationResponse(saved));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <li>Fetches the list of unread entities via a fast, indexed query.</li>
     * <li>Maps the entities to DTOs and dynamically sets {@code isRead = true} using the DTO builder,
     * ensuring the client immediately sees the updated state.</li>
     * <li>Executes a single, efficient JPQL Bulk Update to mark all records as read in the database.</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        List<NotificationResponse> responses = unreadNotifications.stream()
                .map(notificationMapper::toNotificationResponse)
                .map(dto -> dto.toBuilder().isRead(true).build())
                .toList();

        notificationRepository.markAllAsReadByUserId(userId);
        return responses;
    }
}
