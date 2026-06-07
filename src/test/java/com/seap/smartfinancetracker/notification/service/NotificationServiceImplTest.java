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
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    //<editor-fold desc="Setup & Mocks">
    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;
    //</editor-fold>

    //<editor-fold desc="Test createOverdraftNotification">
    @Test
    @DisplayName("Should create overdraft notification and push via SSE")
    void createOverdraftNotification_ShouldSaveAndPushNotification() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User mockUser = new User();

        OverdraftAlertEvent event = OverdraftAlertEvent.builder()
                .userId(userId)
                .categoryName("Food")
                .errorMessage("Insufficient balance")
                .build();

        Notification savedNotification = Instancio.create(Notification.class);
        NotificationResponse responseDto = Instancio.create(NotificationResponse.class);

        when(userRepository.getReferenceById(userId)).thenReturn(mockUser);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toNotificationResponse(savedNotification)).thenReturn(responseDto);

        // Act
        notificationService.createOverdraftNotification(userId, event);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();

        assertEquals(NotificationType.OVERDRAFT_ALERT, capturedNotification.getNotificationType());
        assertTrue(capturedNotification.getMessage().contains("Food"));
        assertTrue(capturedNotification.getMessage().contains("Insufficient balance"));
        assertFalse(capturedNotification.isRead());

        verify(sseNotificationService).pushNotificationToUser(userId, responseDto);
    }
    //</editor-fold>

    //<editor-fold desc="Test createTransactionSuccessNotification">
    @Test
    @DisplayName("Should create transaction success notification for EXPENSE")
    void createTransactionSuccessNotification_ShouldFormatAsSpent_WhenExpense() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User mockUser = new User();

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .userId(userId)
                .categoryName("Groceries")
                .amount(BigDecimal.valueOf(100))
                .transactionType(TransactionType.EXPENSE)
                .build();

        Notification savedNotification = Instancio.create(Notification.class);
        NotificationResponse responseDto = Instancio.create(NotificationResponse.class);

        when(userRepository.getReferenceById(userId)).thenReturn(mockUser);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toNotificationResponse(savedNotification)).thenReturn(responseDto);

        // Act
        notificationService.createTransactionSuccessNotification(userId, event);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();

        assertTrue(capturedNotification.getMessage().contains("spent"));
        verify(sseNotificationService).pushNotificationToUser(userId, responseDto);
    }

    @Test
    @DisplayName("Should create transaction success notification for INCOME")
    void createTransactionSuccessNotification_ShouldFormatAsReceived_WhenIncome() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User mockUser = new User();

        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .userId(userId)
                .categoryName("Salary")
                .amount(BigDecimal.valueOf(500))
                .transactionType(TransactionType.INCOME)
                .build();

        Notification savedNotification = Instancio.create(Notification.class);
        NotificationResponse responseDto = Instancio.create(NotificationResponse.class);

        when(userRepository.getReferenceById(userId)).thenReturn(mockUser);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(notificationMapper.toNotificationResponse(savedNotification)).thenReturn(responseDto);

        // Act
        notificationService.createTransactionSuccessNotification(userId, event);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();

        assertTrue(capturedNotification.getMessage().contains("received"));
        verify(sseNotificationService).pushNotificationToUser(userId, responseDto);
    }
    //</editor-fold>

    //<editor-fold desc="Test getUnreadNotifications">
    @Test
    @DisplayName("Should return unread notifications with isRead updated to true and trigger bulk update")
    void getUnreadNotifications_ShouldReturnMappedListAndMarkAsRead() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Notification unread1 = Instancio.create(Notification.class);
        Notification unread2 = Instancio.create(Notification.class);

        NotificationResponse response1 = Instancio.of(NotificationResponse.class)
                .set(org.instancio.Select.field(NotificationResponse::isRead), false)
                .create();
        NotificationResponse response2 = Instancio.of(NotificationResponse.class)
                .set(org.instancio.Select.field(NotificationResponse::isRead), false)
                .create();

        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(unread1, unread2));

        when(notificationMapper.toNotificationResponse(unread1)).thenReturn(response1);
        when(notificationMapper.toNotificationResponse(unread2)).thenReturn(response2);

        // Act
        List<NotificationResponse> results = notificationService.getUnreadNotifications(userId);

        // Assert
        assertEquals(2, results.size());
        // Verify the mapping logic altered isRead to true
        assertTrue(results.get(0).isRead(), "Mapped DTO should have isRead = true");
        assertTrue(results.get(1).isRead(), "Mapped DTO should have isRead = true");

        // Verify repository bulk update was called
        verify(notificationRepository, times(1)).markAllAsReadByUserId(userId);
    }
    //</editor-fold>
}