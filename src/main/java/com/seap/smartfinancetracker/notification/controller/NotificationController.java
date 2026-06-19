package com.seap.smartfinancetracker.notification.controller;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import com.seap.smartfinancetracker.notification.service.NotificationService;
import com.seap.smartfinancetracker.notification.service.SseNotificationService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing user notifications.
 * <p>
 * This controller provides a hybrid approach to notifications:
 * <ul>
 * <li><b>Real-time Streaming:</b> Provides a Server-Sent Events (SSE) endpoint for unidirectional push notifications.</li>
 * <li><b>RESTful Polling:</b> Provides standard endpoints to fetch historical or missed notifications.</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseNotificationService sseNotificationService;
    private final NotificationService notificationService;

    /**
     * Establishes a Server-Sent Events (SSE) connection for real-time notifications.
     * <p>
     * <b>Client Integration Note:</b> Frontend clients should connect to this endpoint using the
     * native {@code EventSource} API. The connection will remain open, allowing the server to
     * push {@link NotificationResponse} objects directly to the client as events occur.
     * </p>
     *
     * @param userId the authenticated user's ID, automatically injected from the security context
     * @return an {@link SseEmitter} instance maintaining the persistent connection with the client
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotification(@CurrentUserId UUID userId) {
        return sseNotificationService.createEmitter(userId);
    }

    /**
     * Retrieves a list of all unread notifications for the authenticated user.
     * <p>
     * Typically called when the user first logs in or opens the application to catch up
     * on any alerts missed while disconnected from the SSE stream.
     * </p>
     *
     * @param userId the authenticated user's ID
     * @return a {@link ResponseEntity} containing a list of unread {@link NotificationResponse} objects
     */
    @GetMapping(value = "/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotification(@CurrentUserId UUID userId) {
        List<NotificationResponse> unread = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(unread);
    }
}
