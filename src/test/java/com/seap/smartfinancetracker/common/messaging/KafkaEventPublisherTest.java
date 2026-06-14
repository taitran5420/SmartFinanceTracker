package com.seap.smartfinancetracker.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    @Test
    @DisplayName("Should serialize the event and send it to the given topic")
    void publish_ShouldSendSerializedEvent() throws JsonProcessingException {
        // Arrange
        Object event = new Object();
        String payload = "{\"serialized\":\"event\"}";
        when(objectMapper.writeValueAsString(event)).thenReturn(payload);

        // Act
        kafkaEventPublisher.publish("some-topic", event);

        // Assert
        verify(kafkaTemplate, times(1)).send("some-topic", payload);
    }

    @Test
    @DisplayName("Should swallow serialization failure and not send to Kafka")
    void publish_ShouldNotSend_WhenSerializationFails() throws JsonProcessingException {
        // Arrange
        Object event = new Object();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {});

        // Act
        kafkaEventPublisher.publish("some-topic", event);

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
