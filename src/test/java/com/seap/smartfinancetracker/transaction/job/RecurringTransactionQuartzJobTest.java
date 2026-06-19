package com.seap.smartfinancetracker.transaction.job;

import com.seap.smartfinancetracker.transaction.service.RecurringTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionQuartzJobTest {

    @Mock
    private RecurringTransactionService recurringTransactionService;

    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private RecurringTransactionQuartzJob quartzJob;

    //<editor-fold desc="Test executeInternal">
    @Test
    @DisplayName("Should successfully delegate execution to RecurringTransactionService")
    void executeInternal_ShouldCallService() {
        // Act
        // Since executeInternal is protected, we can just call it directly in the same package (or via assertDoesNotThrow)
        assertDoesNotThrow(() -> quartzJob.executeInternal(context));

        // Assert
        verify(recurringTransactionService, times(1)).processDueRecurringTransactions();
    }

    @Test
    @DisplayName("Should wrap any unexpected errors into a Quartz JobExecutionException")
    void executeInternal_ShouldThrowJobExecutionException_WhenServiceFails() {
        // Arrange: Simulate a database failure or unexpected error in the service layer
        doThrow(new RuntimeException("Database timeout")).when(recurringTransactionService).processDueRecurringTransactions();

        // Act & Assert
        assertThrows(
                JobExecutionException.class,
                () -> quartzJob.executeInternal(context),
                "Job must throw JobExecutionException to let the Quartz Scheduler know it failed"
        );
    }
    //</editor-fold>
}