package com.seap.smartfinancetracker.transaction.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventAspectTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionEventAspect transactionEventAspect;

    @Test
    @DisplayName("Should publish serialized event to the transaction-created topic")
    void publishTransactionCreatedEvent_ShouldSendToKafka_WhenResponsePresent() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        TransactionResponse response = Instancio.of(TransactionResponse.class)
                .set(org.instancio.Select.field(TransactionResponse::categoryName), "Groceries")
                .set(org.instancio.Select.field(TransactionResponse::amount), new BigDecimal("12.34"))
                .set(org.instancio.Select.field(TransactionResponse::transactionType), TransactionType.EXPENSE)
                .create();

        String payload = "{\"serialized\":\"event\"}";
        when(objectMapper.writeValueAsString(any(TransactionCreatedEvent.class))).thenReturn(payload);

        // 2. Act
        transactionEventAspect.publishTransactionCreatedEvent(userId, request, response);

        // 3. Assert
        verify(kafkaTemplate, times(1)).send(KafkaConstant.TRANSACTION_CREATED_TOPIC, payload);
    }

    @Test
    @DisplayName("Should swallow serialization failure and not publish to Kafka")
    void publishTransactionCreatedEvent_ShouldNotSend_WhenSerializationFails() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        TransactionResponse response = Instancio.create(TransactionResponse.class);

        when(objectMapper.writeValueAsString(any(TransactionCreatedEvent.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        // 2. Act
        transactionEventAspect.publishTransactionCreatedEvent(userId, request, response);

        // 3. Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Should do nothing when the returned response is null")
    void publishTransactionCreatedEvent_ShouldDoNothing_WhenResponseNull() throws Exception {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);

        // 2. Act
        transactionEventAspect.publishTransactionCreatedEvent(userId, request, null);

        // 3. Assert
        verifyNoInteractions(objectMapper, kafkaTemplate);
    }
}
