package com.seap.smartfinancetracker.transaction.entity;

import com.seap.smartfinancetracker.category.entity.Category;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = Transaction.TRANSACTION_TABLE_NAME, indexes = {
        // Composite index for user and date queries.
        // Covers user_id FK via left-most prefix. If removed, must add a standalone user_id index.
        @Index(name = Transaction.USER_CREATED_AT_INDEX,
                columnList = Transaction.USER_ID_COLUMN_NAME + ", " + Transaction.CREATED_AT_COLUMN_NAME)
})
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    public static final String TRANSACTION_TABLE_NAME = "transactions";

    public static final String USER_ID_COLUMN_NAME = "user_id";
    public static final String CATEGORY_ID_COLUMN_NAME = "category_id";
    public static final String TRANSACTION_TYPE_COLUMN_NAME = "transaction_type";
    public static final String IDEMPOTENCY_KEY_COLUMN_NAME = "idempotency_key";
    public static final String CREATED_AT_COLUMN_NAME = "created_at";
    public static final String UPDATED_AT_COLUMN_NAME = "updated_at";
    public static final String IS_OVER_BUDGET_COLUMN_NAME = "is_over_budget";

    public static final String USER_CREATED_AT_INDEX = "idx_transaction_user_created";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USER_ID_COLUMN_NAME, nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CATEGORY_ID_COLUMN_NAME, nullable = false)
    private Category category;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = TRANSACTION_TYPE_COLUMN_NAME, nullable = false, updatable = false)
    private TransactionType transactionType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = IDEMPOTENCY_KEY_COLUMN_NAME, unique = true, updatable = false)
    private UUID idempotencyKey;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME, nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = IS_OVER_BUDGET_COLUMN_NAME, nullable = false)
    private boolean isOverBudget = false;
}
