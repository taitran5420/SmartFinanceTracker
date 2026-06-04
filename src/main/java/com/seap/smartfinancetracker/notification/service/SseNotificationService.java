package com.seap.smartfinancetracker.notification.service;

import com.seap.smartfinancetracker.notification.dto.NotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface SseNotificationService {
    SseEmitter createEmitter(UUID userId);

    void pushNotificationToUser(UUID userId, NotificationResponse notificationResponse);
}
