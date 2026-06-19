package com.seap.smartfinancetracker.transaction.aspect;

import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.common.messaging.KafkaEventPublisher;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.transaction.constant.TransactionConstant;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionResponse;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.exception.TransactionErrorCode;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionEventAspectTest {

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private TransactionEventAspect transactionEventAspect;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    //<editor-fold desc="@AfterReturning - TransactionCreatedEvent">
    @Test
    @DisplayName("Should publish TransactionCreatedEvent to the transaction-created topic on success")
    void publishTransactionCreatedEvent_ShouldPublish_WhenResponsePresent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        TransactionResponse response = Instancio.of(TransactionResponse.class)
                .set(org.instancio.Select.field(TransactionResponse::categoryName), "Groceries")
                .set(org.instancio.Select.field(TransactionResponse::amount), new BigDecimal("12.34"))
                .set(org.instancio.Select.field(TransactionResponse::transactionType), TransactionType.EXPENSE)
                .create();

        // Act
        transactionEventAspect.publishTransactionCreatedEvent(userId, request, response);

        // Assert
        verify(kafkaEventPublisher, times(1)).publish(eq(KafkaConstant.TRANSACTION_CREATED_TOPIC), eventCaptor.capture());
        TransactionCreatedEvent published = (TransactionCreatedEvent) eventCaptor.getValue();
        assertEquals(userId, published.userId());
        assertEquals("Groceries", published.categoryName());
        assertEquals(new BigDecimal("12.34"), published.amount());
        assertEquals(TransactionType.EXPENSE, published.transactionType());
    }

    @Test
    @DisplayName("Should do nothing when the returned response is null")
    void publishTransactionCreatedEvent_ShouldDoNothing_WhenResponseNull() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);

        // Act
        transactionEventAspect.publishTransactionCreatedEvent(userId, request, null);

        // Assert
        verifyNoInteractions(kafkaEventPublisher);
    }
    //</editor-fold>

    //<editor-fold desc="@AfterThrowing - OverdraftAlertEvent">
    @Test
    @DisplayName("Should publish OverdraftAlertEvent with context category name when overdraft limit is exceeded")
    void publishOverdraftAlertEvent_ShouldPublish_WhenOverdraftException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        BusinessException overdraft = new BusinessException(TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED,
                Map.of(TransactionConstant.OVERDRAFT_CATEGORY_NAME_KEY, "Netflix Subscription"));

        // Act
        transactionEventAspect.publishOverdraftAlertEvent(userId, request, overdraft);

        // Assert
        verify(kafkaEventPublisher, times(1)).publish(eq(KafkaConstant.OVERDRAFT_ALERT_TOPIC), eventCaptor.capture());
        OverdraftAlertEvent published = (OverdraftAlertEvent) eventCaptor.getValue();
        assertEquals(userId, published.userId());
        assertEquals("Netflix Subscription", published.categoryName());
        assertEquals(overdraft.getMessage(), published.errorMessage());
    }

    @Test
    @DisplayName("Should not publish for a non-overdraft BusinessException")
    void publishOverdraftAlertEvent_ShouldNotPublish_WhenDifferentErrorCode() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        BusinessException other = new BusinessException(TransactionErrorCode.IDEMPOTENCY_KEY_EXISTS);

        // Act
        transactionEventAspect.publishOverdraftAlertEvent(userId, request, other);

        // Assert
        verify(kafkaEventPublisher, never()).publish(any(), any());
    }

    @Test
    @DisplayName("Should not publish for an unrelated (non-BusinessException) throwable")
    void publishOverdraftAlertEvent_ShouldNotPublish_WhenNotBusinessException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);

        // Act
        transactionEventAspect.publishOverdraftAlertEvent(userId, request, new RuntimeException("boom"));

        // Assert
        verifyNoInteractions(kafkaEventPublisher);
    }

    @Test
    @DisplayName("Should publish a null category name when the overdraft exception carries no context")
    void publishOverdraftAlertEvent_ShouldHandleMissingContext() {
        // Arrange
        UUID userId = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);
        BusinessException overdraft = new BusinessException(TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED);

        // Act
        transactionEventAspect.publishOverdraftAlertEvent(userId, request, overdraft);

        // Assert
        verify(kafkaEventPublisher, times(1)).publish(eq(KafkaConstant.OVERDRAFT_ALERT_TOPIC), eventCaptor.capture());
        OverdraftAlertEvent published = (OverdraftAlertEvent) eventCaptor.getValue();
        assertEquals(userId, published.userId());
        org.junit.jupiter.api.Assertions.assertNull(published.categoryName());
    }
    //</editor-fold>
}
