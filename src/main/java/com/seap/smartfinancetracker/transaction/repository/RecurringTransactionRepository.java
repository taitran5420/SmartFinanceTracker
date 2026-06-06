package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for managing {@link RecurringTransaction} entities.
 * <p>
 * This interface provides optimized database access methods for automated scheduling,
 * including tenant-isolated lookups and high-performance queries for the Quartz background worker.
 * </p>
 */
@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    Optional<RecurringTransaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Fetches a chronological projection of active automated transactions for a specific user.
     * <p>
     * Used primarily by frontend dashboards to display cash-flow forecasts. The results
     * are strictly ordered by date and time to seamlessly render a timeline view.
     * </p>
     *
     * @param userId    the ID of the target user
     * @param today     the start date of the forecasting window
     * @param next7Days the end date of the forecasting window
     * @return a chronologically sorted list of upcoming {@link RecurringTransaction}s
     */
    @Query("SELECT r FROM RecurringTransaction r " +
            "WHERE r.user.id = :userId " +
            "AND r.active = true " +
            "AND r.nextOccurrenceDate BETWEEN :today AND :next7Days " +
            "ORDER BY r.nextOccurrenceDate ASC, r.executionTime ASC")
    List<RecurringTransaction> findUpComingTransactions(UUID userId, LocalDate today, LocalDate next7Days);

    /**
     * Retrieves all active recurring transactions globally that are currently due for execution.
     * <p>
     * This query acts as the data-source engine for the Quartz scheduler.
     * </p>
     * <p>
     * <b>Time-Drift Resilience:</b> The query condition gracefully catches schedules that are
     * exactly due right now, as well as schedules that were missed in the past (e.g., if the
     * application was offline).
     * </p>
     *
     * @param today the current system date
     * @param now   the current system time
     * @return a complete list of due transactions ready for processing
     */
    @Query("SELECT r FROM RecurringTransaction r " +
            "JOIN FETCH r.user " +
            "JOIN FETCH r.category " +
            "WHERE r.active = true " +
            "AND (" +
            " (r.nextOccurrenceDate < :today) OR " +
            " (r.nextOccurrenceDate = :today AND r.executionTime <= :now)" +
            ")")
    List<RecurringTransaction> findDueTransactions(LocalDate today, LocalTime now);
}
