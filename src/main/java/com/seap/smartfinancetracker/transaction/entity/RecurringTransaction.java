package com.seap.smartfinancetracker.transaction.entity;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = RecurringTransaction.RECURRING_TRANSACTION_TABLE_NAME, indexes = {
        @Index(name = RecurringTransaction.RECURRING_NEXT_OCCURRENCE_DATE_INDEX,
                columnList = RecurringTransaction.NEXT_OCCURRENCE_DATE_COLUMN_NAME + ", " +
                        RecurringTransaction.EXECUTION_TIME_COLUMN_NAME + ", " +
                        RecurringTransaction.ACTIVE_COLUMN_NAME),
        @Index(name = RecurringTransaction.RECURRING_USER_NEXT_DATE_INDEX,
                columnList = RecurringTransaction.USER_ID_COLUMN_NAME + ", " +
                        RecurringTransaction.NEXT_OCCURRENCE_DATE_COLUMN_NAME + ", " +
                        RecurringTransaction.ACTIVE_COLUMN_NAME)
})
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecurringTransaction {

    public static final String RECURRING_TRANSACTION_TABLE_NAME = "recurring_transactions";

    public static final String USER_ID_COLUMN_NAME = "user_id";
    public static final String CATEGORY_ID_COLUMN_NAME = "category_id";
    public static final String TRANSACTION_TYPE_COLUMN_NAME = "transaction_type";
    public static final String START_DATE_COLUMN_NAME = "start_date";
    public static final String END_DATE_COLUMN_NAME = "end_date";
    public static final String ACTIVE_COLUMN_NAME = "active";
    public static final String NEXT_OCCURRENCE_DATE_COLUMN_NAME = "next_occurrence_date";
    public static final String EXECUTION_TIME_COLUMN_NAME = "execution_time";
    public static final String CREATED_AT_COLUMN_NAME = "created_at";
    public static final String UPDATED_AT_COLUMN_NAME = "updated_at";

    public static final String RECURRING_NEXT_OCCURRENCE_DATE_INDEX = "idx_recurring_next_datetime";
    public static final String RECURRING_USER_NEXT_DATE_INDEX = "idx_recurring_user_next_date";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USER_ID_COLUMN_NAME, nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CATEGORY_ID_COLUMN_NAME, nullable = false)
    private Category category;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = TRANSACTION_TYPE_COLUMN_NAME, nullable = false)
    private TransactionType transactionType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = START_DATE_COLUMN_NAME, nullable = false)
    private LocalDate startDate;

    @Column(name = END_DATE_COLUMN_NAME)
    private LocalDate endDate;

    @Column(name = NEXT_OCCURRENCE_DATE_COLUMN_NAME, nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(name = EXECUTION_TIME_COLUMN_NAME, nullable = false)
    private LocalTime executionTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME)
    private Instant updatedAt;

    /**
     * Returns a deterministic idempotency key identifying the occurrence that is currently due
     * (this schedule's {@code id} combined with its {@code nextOccurrenceDate}).
     * <p>
     * The same occurrence always yields the same key, so if an occurrence is processed more than
     * once (e.g. the execution committed but {@code nextOccurrenceDate} was not advanced before a
     * crash, causing the next scheduler run to re-select this row) the duplicate is rejected by the
     * transaction service's idempotency guard instead of charging the user twice. A later occurrence
     * (a different {@code nextOccurrenceDate}) produces a different key and executes normally.
     *
     * @return a stable {@link UUID} for this (schedule, occurrence-date) pair
     */
    public UUID currentOccurrenceIdempotencyKey() {
        String seed = id + ":" + nextOccurrenceDate;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
