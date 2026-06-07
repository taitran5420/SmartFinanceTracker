package com.seap.smartfinancetracker.transaction.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.common.constant.KafkaConstant;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.exception.TransactionErrorCode;
import com.seap.smartfinancetracker.transaction.mapper.RecurringTransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionProcessorTest {

    //<editor-fold desc="Setup & Mocks">
    @Mock
    private TransactionService transactionService;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private RecurringTransactionMapper recurringTransactionMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RecurringTransactionProcessor recurringTransactionProcessor;

    @Captor
    private ArgumentCaptor<RecurringTransaction> recurringCaptor;
    //</editor-fold>

    //<editor-fold desc="Test processSingleRecurringTransaction - Success Scenarios">
    @Test
    @DisplayName("Should successfully execute transaction and increment next occurrence date for MONTHLY frequency")
    void processSingleRecurringTransaction_ShouldExecuteAndUpdateNextDate() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.of(2026, 6, 15);
        Category category = Instancio.create(Category.class);

        RecurringTransaction recurringTransaction = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getCategory), category)
                .set(field(RecurringTransaction::getFrequency), Frequency.MONTHLY)
                .set(field(RecurringTransaction::getNextOccurrenceDate), currentDate)
                .set(field(RecurringTransaction::isActive), true)
                .ignore(field(RecurringTransaction::getEndDate)) // Runs indefinitely
                .create();

        TransactionCreateRequest createRequest = Instancio.create(TransactionCreateRequest.class);

        when(recurringTransactionMapper.toTransactionCreateRequest(recurringTransaction)).thenReturn(createRequest);
        when(recurringTransactionRepository.save(any(RecurringTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        recurringTransactionProcessor.processSingleRecurringTransaction(userId, recurringTransaction);

        // Assert
        verify(transactionService, times(1)).createTransaction(userId, createRequest);
        verify(recurringTransactionRepository).save(recurringCaptor.capture());

        RecurringTransaction updatedTransaction = recurringCaptor.getValue();
        assertEquals(LocalDate.of(2026, 7, 15), updatedTransaction.getNextOccurrenceDate(), "Next date should be incremented by 1 month");
        assertTrue(updatedTransaction.isActive(), "Transaction should remain active");
    }

    @Test
    @DisplayName("Should deactivate recurring transaction after execution if frequency is ONCE")
    void processSingleRecurringTransaction_ShouldDeactivate_WhenFrequencyIsOnce() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecurringTransaction recurringTransaction = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getFrequency), Frequency.ONCE)
                .set(field(RecurringTransaction::isActive), true)
                .create();

        when(recurringTransactionMapper.toTransactionCreateRequest(recurringTransaction)).thenReturn(Instancio.create(TransactionCreateRequest.class));

        // Act
        recurringTransactionProcessor.processSingleRecurringTransaction(userId, recurringTransaction);

        // Assert
        verify(recurringTransactionRepository).save(recurringCaptor.capture());
        assertFalse(recurringCaptor.getValue().isActive(), "Transaction should be deactivated after ONCE execution");
    }

    @Test
    @DisplayName("Should deactivate recurring transaction if next occurrence date surpasses the end date")
    void processSingleRecurringTransaction_ShouldDeactivate_WhenEndDateReached() {
        // Arrange
        UUID userId = UUID.randomUUID();
        LocalDate currentDate = LocalDate.of(2026, 12, 28);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        RecurringTransaction recurringTransaction = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getFrequency), Frequency.WEEKLY)
                .set(field(RecurringTransaction::getNextOccurrenceDate), currentDate)
                .set(field(RecurringTransaction::getEndDate), endDate)
                .set(field(RecurringTransaction::isActive), true)
                .create();

        when(recurringTransactionMapper.toTransactionCreateRequest(recurringTransaction)).thenReturn(Instancio.create(TransactionCreateRequest.class));

        // Act
        recurringTransactionProcessor.processSingleRecurringTransaction(userId, recurringTransaction);

        // Assert
        verify(recurringTransactionRepository).save(recurringCaptor.capture());
        RecurringTransaction updatedTransaction = recurringCaptor.getValue();

        // Next date = 2026-12-28 + 1 week = 2027-01-04 (which is after 2026-12-31)
        assertEquals(LocalDate.of(2027, 1, 4), updatedTransaction.getNextOccurrenceDate());
        assertFalse(updatedTransaction.isActive(), "Transaction should be deactivated because next occurrence is after end date");
    }
    //</editor-fold>

    //<editor-fold desc="Test processSingleRecurringTransaction - Exception Scenarios">
    @Test
    @DisplayName("Should catch OVERDRAFT exception and publish event to Kafka, while still updating lifecycle")
    void processSingleRecurringTransaction_ShouldHandleOverdraftAndSendKafkaEvent() throws JsonProcessingException {
        // Arrange
        UUID userId = UUID.randomUUID();
        Category category = Instancio.of(Category.class).set(field(Category::getCategoryName), "Netflix Subscription").create();

        RecurringTransaction recurringTransaction = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getCategory), category)
                .set(field(RecurringTransaction::getFrequency), Frequency.MONTHLY)
                .create();

        TransactionCreateRequest createRequest = Instancio.create(TransactionCreateRequest.class);
        String mockJsonPayload = "{\"userId\":\"" + userId + "\", \"categoryName\":\"Netflix Subscription\"}";

        when(recurringTransactionMapper.toTransactionCreateRequest(recurringTransaction)).thenReturn(createRequest);
        when(objectMapper.writeValueAsString(any())).thenReturn(mockJsonPayload);

        // Simulate an overdraft error thrown by the inner TransactionService
        doThrow(new BusinessException(TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED))
                .when(transactionService).createTransaction(userId, createRequest);

        // Act
        // This should NOT throw an exception up the stack because it's caught and isolated by the processor
        assertDoesNotThrow(() -> recurringTransactionProcessor.processSingleRecurringTransaction(userId, recurringTransaction));

        // Assert
        // Verify Kafka event was dispatched to the correct topic
        verify(kafkaTemplate, times(1)).send(eq(KafkaConstant.OVERDRAFT_ALERT_TOPIC), eq(mockJsonPayload));

        // Verify lifecycle was STILL updated (so it can retry next month)
        verify(recurringTransactionRepository, times(1)).save(any(RecurringTransaction.class));
    }

    @Test
    @DisplayName("Should silently catch generic exceptions to prevent batch processing failure")
    void processSingleRecurringTransaction_ShouldCatchGenericExceptions() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecurringTransaction recurringTransaction = Instancio.create(RecurringTransaction.class);

        when(recurringTransactionMapper.toTransactionCreateRequest(any())).thenThrow(new RuntimeException("Unexpected Database Error"));

        // Act
        // The processor must catch all exceptions to ensure other users' transactions in the same batch aren't blocked
        assertDoesNotThrow(() -> recurringTransactionProcessor.processSingleRecurringTransaction(userId, recurringTransaction));

        // Assert
        // Kafka shouldn't be called for non-overdraft errors
        verifyNoInteractions(kafkaTemplate);

        // Lifecycle still updates to move past the failing record
        verify(recurringTransactionRepository, times(1)).save(any(RecurringTransaction.class));
    }
    //</editor-fold>
}