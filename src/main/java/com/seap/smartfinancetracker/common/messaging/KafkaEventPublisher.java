package com.seap.smartfinancetracker.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Single entry point for publishing domain events to Kafka.
 * <p>
 * Centralizes the "serialize the event to JSON, send it to a topic, and log on failure" mechanics
 * so callers (e.g. aspects) never deal with {@link ObjectMapper} or {@link KafkaTemplate} directly.
 * Serialization failures are swallowed and logged: a missed notification must never break the
 * business operation that produced the event.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Serializes {@code event} to a JSON string and sends it to the given Kafka {@code topic}.
     *
     * @param topic the destination Kafka topic
     * @param event the event payload to serialize and publish
     */
    public void publish(String topic, Object event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, jsonPayload);
            log.info("Published event to Kafka topic '{}': {}", topic, event.getClass().getSimpleName());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for Kafka topic '{}'", topic, e);
        }
    }
}
