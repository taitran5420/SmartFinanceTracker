package com.seap.smartfinancetracker.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event listener component responsible for consuming asynchronous messages from Apache Kafka.
 * <p>
 * This class acts as the primary entry point for the Notification module within the
 * Event-Driven Architecture (EDA). It listens to predefined Kafka topics, deserializes
 * the incoming JSON payloads into strongly-typed Java records, and delegates the actual
 * notification delivery logic to the {@link NotificationService}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes messages from the Overdraft Alert Kafka topic.
     * <p>
     * Triggered when a user violates their budget constraints. The method attempts to
     * parse the raw JSON string back into an {@link OverdraftAlertEvent} and dispatches
     * it to the notification service to generate a warning alert.
     * </p>
     *
     * @param eventJsonString the raw JSON payload consumed from the Kafka broker
     */
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

    /**
     * Consumes messages from the Transaction Created Kafka topic.
     * <p>
     * Triggered immediately after a new financial transaction is successfully persisted.
     * Parses the JSON payload into a {@link TransactionCreatedEvent} and triggers the
     * notification service to send a real-time success confirmation to the user.
     * </p>
     *
     * @param eventJsonString the raw JSON payload consumed from the Kafka broker
     */
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
