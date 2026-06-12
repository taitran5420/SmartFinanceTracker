package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * Fetches a page of transactions matching the given specification, eagerly loading
     * each transaction's {@link com.seap.smartfinancetracker.category.entity.Category}.
     * <p>
     * Overrides {@link JpaSpecificationExecutor#findAll(Specification, Pageable)} with an
     * {@link EntityGraph} so that mapping each row's {@code categoryName} does not trigger
     * an N+1 lazy load of the (LAZY) category association.
     * </p>
     *
     * @param spec     the dynamic filter specification
     * @param pageable the pagination information
     * @return a page of matching transactions with their categories initialized
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

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
}
