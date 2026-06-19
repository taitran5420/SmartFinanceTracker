package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SseNotificationServiceImplTest {

    @InjectMocks
    private SseNotificationServiceImpl sseNotificationService;

    //<editor-fold desc="Test createEmitter">
    @Test
    @DisplayName("Should create and register SseEmitter successfully")
    void createEmitter_ShouldReturnSseEmitter() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        SseEmitter emitter = sseNotificationService.createEmitter(userId);

        // Assert
        assertNotNull(emitter, "Emitter should not be null");
        assertNotNull(emitter.getTimeout(), "Emitter timeout should be configured");
    }
    //</editor-fold>

    //<editor-fold desc="Test pushNotificationToUser">
    @Test
    @DisplayName("Should silently drop notification if user is not connected (no emitter)")
    void pushNotificationToUser_ShouldDoNothing_WhenUserNotConnected() {
        // Arrange
        UUID disconnectedUserId = UUID.randomUUID();
        NotificationResponse response = Instancio.create(NotificationResponse.class);

        // Act & Assert
        assertDoesNotThrow(() -> sseNotificationService.pushNotificationToUser(disconnectedUserId, response));
    }

    @Test
    @DisplayName("Should attempt to push notification when user is connected")
    void pushNotificationToUser_ShouldSend_WhenUserConnected() {
        // Arrange
        UUID connectedUserId = UUID.randomUUID();
        NotificationResponse response = Instancio.create(NotificationResponse.class);

        // Register the user first
        sseNotificationService.createEmitter(connectedUserId);

        // Act & Assert
        assertDoesNotThrow(() -> sseNotificationService.pushNotificationToUser(connectedUserId, response));
    }
    //</editor-fold>
}