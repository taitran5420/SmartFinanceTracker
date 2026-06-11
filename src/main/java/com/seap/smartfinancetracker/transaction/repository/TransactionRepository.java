package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for managing {@link Transaction} entities.
 * <p>
 * This interface provides standard CRUD operations, dynamic query execution
 * via {@link JpaSpecificationExecutor}, and custom queries for financial aggregations
 * and data isolation (tenant-like scoping by user ID).
 * </p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    /**
     * Calculates the total sum of amounts for a specific user and transaction type.
     * <p>
     * <b>Note:</b> This query only includes active (non-deleted) transactions.
     * It safely utilizes {@code COALESCE} to return {@code 0.0} instead of {@code null}
     * if the user has no transactions of the specified type.
     * </p>
     *
     * @param userId          the unique identifier of the user
     * @param transactionType the classification to sum up (e.g., INCOME or EXPENSE)
     * @return the total aggregated amount, guaranteed to be non-null
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.transactionType = :transactionType " +
            "AND t.active = true")
    BigDecimal calculateTotalAmountByUserIdAndTransactionType(UUID userId, TransactionType transactionType);

    /**
     * Calculates the total amount spent by a specific user in a given category
     * during a specific month and year.
     * <p>
     * <b>Query Details:</b>
     * <ul>
     * <li>Filters for transactions implicitly defined as {@code 'EXPENSE'}.</li>
     * <li>Only includes active (non-deleted) transactions.</li>
     * <li>Safely utilizes {@code COALESCE} to return {@code 0.0} instead of {@code null}
     * if the user has no transactions matching the criteria.</li>
     * </ul>
     * </p>
     *
     * @param userId     the unique identifier of the user who owns the transactions
     * @param categoryId the unique identifier of the category to aggregate
     * @param month      the numeric representation of the month
     * @param year       the four-digit representation of the year
     * @return the total aggregated spent amount, guaranteed to be non-null
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM  Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.category.id = :categoryId " +
            "AND t.transactionType = 'EXPENSE' " +
            "AND t.active = true " +
            "AND EXTRACT(MONTH FROM t.createdAt) = :month " +
            "AND EXTRACT(YEAR FROM t.createdAt) = :year ")
    BigDecimal calculateTotalSpentByCategoryAndMonth(UUID userId, UUID categoryId, int month, int year);

    /**
     * Produces a per-month, per-type breakdown of a user's active transactions within an
     * optional, inclusive date range.
     * <p>
     * Rows are grouped by calendar year, calendar month, and {@link TransactionType}, then
     * ordered chronologically. The month/year are derived from {@code createdAt} via SQL
     * {@code EXTRACT}, which operates in UTC; callers that need a different time zone should
     * account for boundary differences. Each row is projected onto
     * {@link MonthlyTrendProjection}.
     * </p>
     *
     * @param userId    the unique identifier of the user
     * @param startDate the inclusive lower bound of the range, or {@code null} for no lower bound
     * @param endDate   the inclusive upper bound of the range, or {@code null} for no upper bound
     * @return a chronologically ordered list of monthly per-type totals
     */
    @Query("SELECT EXTRACT(YEAR FROM t.createdAt) AS year, " +
            "EXTRACT(MONTH FROM t.createdAt) AS month, " +
            "t.transactionType AS transactionType, " +
            "COALESCE(SUM(t.amount), 0) AS totalAmount " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.active = true " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR t.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR t.createdAt <= :endDate) " +
            "GROUP BY EXTRACT(YEAR FROM t.createdAt), EXTRACT(MONTH FROM t.createdAt), t.transactionType " +
            "ORDER BY EXTRACT(YEAR FROM t.createdAt), EXTRACT(MONTH FROM t.createdAt)")
    List<MonthlyTrendProjection> findMonthlyTrend(UUID userId, Instant startDate, Instant endDate);

    /**
     * Computes a user's income total, expense total, and transaction count for an optional,
     * inclusive date range in a single pass over the data.
     * <p>
     * Income and expense are accumulated with conditional aggregation ({@code CASE}) so the
     * three figures required by a period summary are produced by one query rather than three.
     * Only active (non-deleted) transactions are included, and {@code COALESCE} guarantees
     * non-null totals. {@code null} bounds disable that side of the range.
     * </p>
     *
     * @param userId    the unique identifier of the user
     * @param startDate the inclusive lower bound of the range, or {@code null} for no lower bound
     * @param endDate   the inclusive upper bound of the range, or {@code null} for no upper bound
     * @return a projection carrying the income total, expense total, and transaction count
     */
    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'INCOME' THEN t.amount ELSE 0 END), 0) AS totalIncome, " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS totalExpense, " +
            "COUNT(t) AS transactionCount " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.active = true " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR t.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR t.createdAt <= :endDate)")
    PeriodTotalsProjection calculatePeriodTotals(UUID userId, Instant startDate, Instant endDate);

    /**
     * Returns expense totals grouped by category, joined with the category name and ordered
     * by descending spend, for an optional inclusive date range.
     * <p>
     * Joining the name into the aggregate avoids a second lookup to resolve category names.
     * The {@code pageable} argument bounds how many rows are returned: pass
     * {@code PageRequest.of(0, 1)} to fetch only the top-spending category, or
     * {@code Pageable.unpaged()} for the full breakdown. Only active {@code EXPENSE}
     * transactions are considered, and {@code null} date bounds disable that side of the range.
     * </p>
     *
     * @param userId    the unique identifier of the user
     * @param startDate the inclusive lower bound of the range, or {@code null} for no lower bound
     * @param endDate   the inclusive upper bound of the range, or {@code null} for no upper bound
     * @param pageable  bounds the number of returned rows (e.g. top-N by spend)
     * @return per-category spending details (id, name, total), ordered from highest to lowest
     */
    @Query("SELECT t.category.id AS categoryId, " +
            "t.category.categoryName AS categoryName, " +
            "COALESCE(SUM(t.amount), 0) AS totalSpent " +
            "FROM Transaction t " +
            "WHERE t.user.id = :userId " +
            "AND t.transactionType = 'EXPENSE' " +
            "AND t.active = true " +
            "AND (CAST(:startDate AS timestamp) IS NULL OR t.createdAt >= :startDate) " +
            "AND (CAST(:endDate AS timestamp) IS NULL OR t.createdAt <= :endDate) " +
            "GROUP BY t.category.id, t.category.categoryName " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<CategorySpendingProjection> findCategorySpending(UUID userId,
                                                          Instant startDate,
                                                          Instant endDate,
                                                          Pageable pageable);
}
