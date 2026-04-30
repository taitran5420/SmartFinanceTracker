package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.transaction.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Slice<Transaction> findSliceByUserIdAndCreatedAtBetween(UUID userId, Instant startDate, Instant endDate, Pageable pageable);

    boolean existsByIdempotencyKey(UUID idempotencyKey);
}
