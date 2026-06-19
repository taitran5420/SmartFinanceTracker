package com.seap.smartfinancetracker.transaction.mapper;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Component responsible for mapping between {@link RecurringTransaction} entities
 * and their corresponding Data Transfer Objects (DTOs).
 * <p>
 * This class handles the data transformation logic for the automated scheduling module,
 * strictly separating the database entity structure from the API and internal communication payloads.
 * </p>
 */
@Component
public class RecurringTransactionMapper {

    /**
     * Converts a persisted {@link RecurringTransaction} entity into a standard REST response.
     *
     * @param recurringTransaction the entity retrieved from the database
     * @return the mapped {@link RecurringTransactionResponse}, or {@code null} if the entity is null
     */
    public RecurringTransactionResponse toRecurringTransactionResponse(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return RecurringTransactionResponse.builder()
                .id(recurringTransaction.getId())
                .categoryId(recurringTransaction.getCategory().getId())
                .categoryName(recurringTransaction.getCategory().getCategoryName())
                .amount(recurringTransaction.getAmount())
                .transactionType(recurringTransaction.getTransactionType())
                .note(recurringTransaction.getNote())
                .frequency(recurringTransaction.getFrequency())
                .startDate(recurringTransaction.getStartDate())
                .endDate(recurringTransaction.getEndDate())
                .nextOccurrenceDate(recurringTransaction.getNextOccurrenceDate())
                .executionTime(recurringTransaction.getExecutionTime())
                .active(recurringTransaction.isActive())
                .build();
    }

    /**
     * Converts a creation request into a new {@link RecurringTransaction} entity.
     * <p>
     * <b>Initialization Notes:</b>
     * <ul>
     * <li>Automatically initializes the {@code active} flag to {@code true}.</li>
     * <li>Sets the initial {@code nextOccurrenceDate} to match the requested {@code startDate}.</li>
     * </ul>
     * </p>
     *
     * @param userId                            the ID of the user creating the schedule
     * @param recurringTransactionCreateRequest the payload containing the scheduling rules
     * @param category                          the fully fetched category entity to inherit the transaction type
     * @return a new {@link RecurringTransaction} entity ready for persistence
     */
    public RecurringTransaction toEntity(UUID userId, RecurringTransactionCreateRequest recurringTransactionCreateRequest, Category category) {
        if (recurringTransactionCreateRequest == null) {
            return null;
        }

        return RecurringTransaction.builder()
                .user(User.builder().id(userId).build())
                .category(category)
                .amount(recurringTransactionCreateRequest.amount())
                .transactionType(category.getTransactionType())
                .note(recurringTransactionCreateRequest.note())
                .frequency(recurringTransactionCreateRequest.frequency())
                .startDate(recurringTransactionCreateRequest.startDate())
                .endDate(recurringTransactionCreateRequest.endDate())
                .executionTime(recurringTransactionCreateRequest.executionTime())
                .active(true)
                .nextOccurrenceDate(recurringTransactionCreateRequest.startDate())
                .build();
    }

    /**
     * Converts a recurring transaction entity into a specialized forecasting projection.
     * <p>
     * <b>Dynamic Calculation:</b> Calculates the {@code daysUntilDue} on-the-fly using
     * the system's current date against the scheduled {@code nextOccurrenceDate}.
     * This offloads time-math from the frontend clients.
     * </p>
     *
     * @param recurringTransaction the scheduled transaction entity
     * @return an {@link UpcomingRecurringResponse} optimized for dashboard display or
     * {@code null} if the recurring transaction is null
     */
    public UpcomingRecurringResponse toUpcomingRecurringResponse(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return UpcomingRecurringResponse.builder()
                .id(recurringTransaction.getId())
                .categoryName(recurringTransaction.getCategory().getCategoryName())
                .amount(recurringTransaction.getAmount())
                .frequency(recurringTransaction.getFrequency())
                .nextOccurrenceDate(recurringTransaction.getNextOccurrenceDate())
                .executionTime(recurringTransaction.getExecutionTime())
                .daysUntilDue(ChronoUnit.DAYS.between(LocalDate.now(), recurringTransaction.getNextOccurrenceDate()))
                .build();
    }

    /**
     * Converts a mature recurring configuration into an actionable transaction creation request.
     * <p>
     * <b>Architecture Note:</b> When a recurring schedule is due, it does not bypass the core
     * transaction module. Instead, this mapper generates a {@link TransactionCreateRequest}
     * so the automated execution strictly adheres to standard validation and double-spending
     * protection rules.
     * </p>
     * <p>
     * <b>Double-charge protection:</b> The {@code idempotencyKey} is the schedule's deterministic
     * per-occurrence key ({@link RecurringTransaction#currentOccurrenceIdempotencyKey()}). If the
     * same occurrence is processed more than once (e.g. the schedule was executed but its
     * {@code nextOccurrenceDate} was not advanced before a crash, so the next Quartz run re-selects
     * it), the identical key causes the core service to reject the duplicate instead of creating a
     * second transaction.
     * </p>
     *
     * @param recurringTransaction the scheduled configuration that is currently due
     * @return a payload simulating a user creating a new transaction
     */
    public TransactionCreateRequest toTransactionCreateRequest(RecurringTransaction recurringTransaction) {
        if (recurringTransaction == null) {
            return null;
        }

        return TransactionCreateRequest.builder()
                .categoryId(recurringTransaction.getCategory().getId())
                .amount(recurringTransaction.getAmount())
                .transactionType(recurringTransaction.getTransactionType())
                .note(recurringTransaction.getNote())
                .idempotencyKey(recurringTransaction.currentOccurrenceIdempotencyKey())
                .build();
    }
}
