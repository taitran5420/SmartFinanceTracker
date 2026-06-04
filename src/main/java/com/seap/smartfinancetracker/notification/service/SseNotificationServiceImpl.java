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

@Slf4j
@Service
public class SseNotificationServiceImpl implements SseNotificationService {

    private final Map<UUID, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(1800000L);

        userEmitters.put(userId, emitter);
        log.info("SSE connection established for user: {}", userId);

        emitter.onCompletion(() -> removeEmitter(userId));
        emitter.onTimeout(() -> removeEmitter(userId));
        emitter.onError((e) -> removeEmitter(userId));

        return emitter;
    }

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

    private void removeEmitter(UUID userId) {
        userEmitters.remove(userId);
        log.debug("SSE connection removed for user: {}", userId);
    }
}
