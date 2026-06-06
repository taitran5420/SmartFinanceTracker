package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.dto.UpcomingRecurringResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business logic contract for managing recurring transactions.
 * <p>
 * This service handles the complete lifecycle of automated financial schedules, including
 * creation, partial updates, toggling states, and triggering the background processing engine.
 * </p>
 */
public interface RecurringTransactionService {

    /**
     * Creates a new recurring transaction schedule.
     *
     * @param userId  the ID of the user creating the schedule
     * @param request the configuration payload
     * @return the fully configured {@link RecurringTransactionResponse}
     */
    RecurringTransactionResponse createRecurring(UUID userId, RecurringTransactionCreateRequest request);

    /**
     * Retrieves a specific schedule while enforcing user ownership.
     *
     * @param userId the ID of the requesting user
     * @param id     the unique identifier of the recurring transaction
     * @return the requested {@link RecurringTransactionResponse}
     */
    RecurringTransactionResponse getRecurringById(UUID userId, UUID id);

    /**
     * Partially updates an existing schedule.
     *
     * @param userId  the ID of the requesting user
     * @param id      the identifier of the schedule to update
     * @param request the payload containing only the fields to be modified
     * @return the updated {@link RecurringTransactionResponse}
     */
    RecurringTransactionResponse updateRecurring(UUID userId, UUID id, RecurringTransactionUpdateRequest request);

    /**
     * Permanently deletes a recurring schedule.
     *
     * @param userId the ID of the requesting user
     * @param id     the identifier of the schedule to delete
     */
    void deleteRecurring(UUID userId, UUID id);

    /**
     * Toggles the active/paused state of a schedule.
     *
     * @param userId the ID of the requesting user
     * @param id     the identifier of the schedule to toggle
     * @return the updated {@link RecurringTransactionResponse} reflecting the new state
     */
    RecurringTransactionResponse toggleActiveStatus(UUID userId, UUID id);

    /**
     * Generates a 7-day forecast of upcoming automated transactions.
     *
     * @param userId the ID of the user requesting the forecast
     * @return a list of {@link UpcomingRecurringResponse} forecasts
     */
    List<UpcomingRecurringResponse> getUpcomingTransactions(UUID userId);

    /**
     * The core trigger for the automation engine.
     * <p>
     * Fetches all globally due schedules, groups them by user, and dispatches them
     * to the asynchronous processing pool for execution.
     * </p>
     */
    void processDueRecurringTransactions();
}