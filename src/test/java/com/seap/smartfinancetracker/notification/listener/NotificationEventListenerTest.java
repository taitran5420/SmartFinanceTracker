package com.seap.smartfinancetracker.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.notification.event.OverdraftAlertEvent;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.notification.service.NotificationService;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    //<editor-fold desc="Setup & Configurations">
    @Mock
    private NotificationService notificationService;

    // Use a real ObjectMapper to thoroughly test the JSON deserialization process
    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationEventListener notificationEventListener;

    @BeforeEach
    void setUp() {
        notificationEventListener = new NotificationEventListener(notificationService, objectMapper);
    }
    //</editor-fold>

    //<editor-fold desc="Test handleOverdraftAlertEvent">
    @Test
    @DisplayName("Should successfully parse JSON and call NotificationService for OverdraftAlertEvent")
    void handleOverdraftAlertEvent_ShouldProcessSuccessfully() throws JsonProcessingException {
        // Arrange: Generate a fully populated event object dynamically using Instancio
        OverdraftAlertEvent event = Instancio.create(OverdraftAlertEvent.class);
        String jsonPayload = objectMapper.writeValueAsString(event);

        // Act
        notificationEventListener.handleOverdraftAlertEvent(jsonPayload);

        // Assert: Verify the service is called with the dynamically generated UUID
        verify(notificationService, times(1)).createOverdraftNotification(eq(event.userId()), any(OverdraftAlertEvent.class));
    }

    @Test
    @DisplayName("Should catch JsonProcessingException and NOT call service when JSON is invalid")
    void handleOverdraftAlertEvent_ShouldCatchJsonException() {
        // Arrange: Purposely malformed JSON string
        String invalidJsonPayload = "{ invalid_json: missing_quotes }";

        // Act
        notificationEventListener.handleOverdraftAlertEvent(invalidJsonPayload);

        // Assert: Service should never be triggered if parsing fails
        verify(notificationService, never()).createOverdraftNotification(any(), any());
    }

    @Test
    @DisplayName("Should catch general Exception when Service throws an error")
    void handleOverdraftAlertEvent_ShouldCatchGeneralException() throws JsonProcessingException {
        // Arrange: Generate event using Instancio
        OverdraftAlertEvent event = Instancio.create(OverdraftAlertEvent.class);
        String jsonPayload = objectMapper.writeValueAsString(event);

        // Simulate an unexpected system error (e.g., Database down) in the service layer
        doThrow(new RuntimeException("Database down")).when(notificationService).createOverdraftNotification(any(), any());

        // Act
        notificationEventListener.handleOverdraftAlertEvent(jsonPayload);

        // Assert: Verify the service was called but the exception was caught gracefully by the listener
        verify(notificationService, times(1)).createOverdraftNotification(eq(event.userId()), any(OverdraftAlertEvent.class));
    }
    //</editor-fold>

    //<editor-fold desc="Test handleTransactionCreatedEvent">
    @Test
    @DisplayName("Should successfully parse JSON and call NotificationService for TransactionCreatedEvent")
    void handleTransactionCreatedEvent_ShouldProcessSuccessfully() throws JsonProcessingException {
        // Arrange: Generate a fully populated event object dynamically using Instancio
        TransactionCreatedEvent event = Instancio.create(TransactionCreatedEvent.class);
        String jsonPayload = objectMapper.writeValueAsString(event);

        // Act
        notificationEventListener.handleTransactionCreatedEvent(jsonPayload);

        // Assert
        verify(notificationService, times(1)).createTransactionSuccessNotification(eq(event.userId()), any(TransactionCreatedEvent.class));
    }

    @Test
    @DisplayName("Should catch JsonProcessingException and NOT call service when JSON is invalid for Transaction")
    void handleTransactionCreatedEvent_ShouldCatchJsonException() {
        // Arrange: Invalid non-JSON string
        String invalidJsonPayload = "Not a JSON string at all";

        // Act
        notificationEventListener.handleTransactionCreatedEvent(invalidJsonPayload);

        // Assert
        verify(notificationService, never()).createTransactionSuccessNotification(any(), any());
    }

    @Test
    @DisplayName("Should catch general Exception when Service throws an error for Transaction")
    void handleTransactionCreatedEvent_ShouldCatchGeneralException() throws JsonProcessingException {
        // Arrange: Generate event using Instancio
        TransactionCreatedEvent event = Instancio.create(TransactionCreatedEvent.class);
        String jsonPayload = objectMapper.writeValueAsString(event);

        // Simulate service failure
        doThrow(new RuntimeException("Unexpected Error")).when(notificationService).createTransactionSuccessNotification(any(), any());

        // Act
        notificationEventListener.handleTransactionCreatedEvent(jsonPayload);

        // Assert: Ensure execution didn't crash the listener thread
        verify(notificationService, times(1)).createTransactionSuccessNotification(eq(event.userId()), any(TransactionCreatedEvent.class));
    }
    //</editor-fold>
}