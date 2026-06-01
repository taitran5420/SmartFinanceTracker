package com.seap.smartfinancetracker.notification.controller;

import com.seap.smartfinancetracker.notification.entity.Notification;
import com.seap.smartfinancetracker.notification.repository.NotificationRepository;
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

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseNotificationService sseNotificationService;

    private final NotificationRepository notificationRepository;

    @GetMapping(value = "/subcribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotification(@CurrentUserId UUID userId) {
        return sseNotificationService.createEmitter(userId);
    }

    @GetMapping(value = "/unread")
    public ResponseEntity<List<Notification>> getUnreadNotification(@CurrentUserId UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(unread);
    }
}
