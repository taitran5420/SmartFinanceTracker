package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {


    @Query("SELECT r FROM RecurringTransaction r " +
            "WHERE r.user.id = :userId " +
            "AND r.active = true " +
            "AND r.nextOccurrenceDate BETWEEN :today AND :next7Days " +
            "ORDER BY r.nextOccurrenceDate ASC, r.executionTime ASC")
    List<RecurringTransaction> findUpComingTransactions(UUID userId, LocalDate today, LocalDate next7Days);

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
