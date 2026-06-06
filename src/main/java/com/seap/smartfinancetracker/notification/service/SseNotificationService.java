package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Service interface defining the contract for real-time Server-Sent Events (SSE) management.
 * <p>
 * This service handles the lifecycle of persistent HTTP connections with frontend clients
 * and provides the mechanism to push unidirectional notification events directly to active users.
 * </p>
 */
public interface SseNotificationService {

    /**
     * Initializes and registers a new SSE connection for a specific user.
     *
     * @param userId the unique identifier of the user establishing the connection
     * @return an {@link SseEmitter} instance configured for the user's session
     */
    SseEmitter createEmitter(UUID userId);

    /**
     * Asynchronously pushes a notification payload to a connected user's active SSE stream.
     * <p>
     * If the user is not currently connected, the push operation is safely bypassed.
     * </p>
     *
     * @param userId               the unique identifier of the target user
     * @param notificationResponse the notification payload to be serialized and sent
     */
    void pushNotificationToUser(UUID userId, NotificationResponse notificationResponse);
}
