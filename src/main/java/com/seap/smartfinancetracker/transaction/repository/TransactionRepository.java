package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
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
}
