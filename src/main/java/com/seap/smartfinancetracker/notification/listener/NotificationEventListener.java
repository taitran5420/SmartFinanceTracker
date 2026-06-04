package com.seap.smartfinancetracker.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.kafka.constant.KafkaConstant;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConstant.OVERDRAFT_ALERT_TOPIC, groupId = KafkaConstant.GROUP_ID)
    public void handleOverdraftAlertEvent(String eventJsonString) {
        try {
            OverdraftAlertEvent overdraftAlertEvent = objectMapper.readValue(eventJsonString, OverdraftAlertEvent.class);
            log.debug("Async listener caught OverdraftAlertEvent for user: {}", overdraftAlertEvent.userId());
            notificationService.createOverdraftNotification(overdraftAlertEvent.userId(), overdraftAlertEvent);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message into Object. Message: {}", eventJsonString, e);
        } catch (Exception e) {
            log.error("Failed to process Kafka success notification", e);
        }
    }

    @KafkaListener(topics = KafkaConstant.TRANSACTION_CREATED_TOPIC, groupId = KafkaConstant.GROUP_ID)
    public void handleTransactionCreatedEvent(String eventJsonString) {
        try {
            TransactionCreatedEvent transactionCreatedEvent = objectMapper.readValue(eventJsonString, TransactionCreatedEvent.class);
            log.debug("Async listener caught TransactionCreatedEvent for user: {}", transactionCreatedEvent.userId());
            notificationService.createTransactionSuccessNotification(transactionCreatedEvent.userId(), transactionCreatedEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message into Object. Message: {}", eventJsonString, e);
        } catch (Exception e) {
            log.error("Failed to process Kafka success notification", e);
        }
    }
}
