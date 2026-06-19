package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.common.config.ThreadPoolConfig;
import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core implementation of the {@link SseNotificationService}.
 */
@Slf4j
@Service
public class SseNotificationServiceImpl implements SseNotificationService {

    private final Map<UUID, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * Configures the emitter with a 30-minute timeout (1,800,000 ms) and attaches crucial
     * lifecycle hooks (completion, timeout, error) to gracefully remove stale connections
     * and prevent memory leaks.
     * </p>
     */
    public SseEmitter createEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(1800000L);

        userEmitters.put(userId, emitter);
        log.info("SSE connection established for user: {}", userId);

        emitter.onCompletion(() -> removeEmitter(userId));
        emitter.onTimeout(() -> removeEmitter(userId));
        emitter.onError((e) -> removeEmitter(userId));

        return emitter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Performance & Resilience:</b>
     * Executed asynchronously using a dedicated thread pool ({@code NOTIFICATION_EXECUTOR_BEAN_NAME}).
     * This isolates the potential network latency of the SSE dispatch from the main application
     * or Kafka consumer threads. If a dispatch fails due to network issues (IOException),
     * the stale connection is automatically pruned from the registry.
     * </p>
     */
    @Async(ThreadPoolConfig.NOTIFICATION_EXECUTOR_BEAN_NAME)
    public void pushNotificationToUser(UUID userId, NotificationResponse notificationResponse) {
        SseEmitter emitter = userEmitters.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-notification")
                        .data(notificationResponse, MediaType.APPLICATION_JSON));
                log.info("Successfully pushed real-time notification to user: {}", userId);
            } catch (IOException e) {
                log.error("Failed to push notification. Removing stale emitter for user: {}", userId);
                removeEmitter(userId);
            }
        } else {
            log.debug("User {} is not currently connected. Notification saved to DB only.", userId);
        }
    }

    /**
     * Safely removes an emitter from the active connection registry.
     *
     * @param userId the unique identifier of the disconnected user
     */
    private void removeEmitter(UUID userId) {
        userEmitters.remove(userId);
        log.debug("SSE connection removed for user: {}", userId);
    }
}
