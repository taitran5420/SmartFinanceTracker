package com.seap.smartfinancetracker.budget.entity;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing a financial budget in the database.
 */
@Entity
@Table(name = Budget.BUDGET_TABLE_NAME, uniqueConstraints = {
        @UniqueConstraint(name = Budget.USER_CATEGORY_MONTH_YEAR_UNIQUE_CONSTRAINT,
                columnNames = {Budget.USER_ID_COLUMN_NAME, Budget.CATEGORY_ID_COLUMN_NAME,
                        Budget.BUDGET_MONTH_COLUMN_NAME, Budget.BUDGET_YEAR_COLUMN_NAME})
})
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Budget {

    public static final String BUDGET_TABLE_NAME = "budgets";

    public static final String USER_ID_COLUMN_NAME = "user_id";
    public static final String CATEGORY_ID_COLUMN_NAME = "category_id";
    public static final String AMOUNT_LIMIT_COLUMN_NAME = "amount_limit";
    public static final String BUDGET_MONTH_COLUMN_NAME = "budget_month";
    public static final String BUDGET_YEAR_COLUMN_NAME = "budget_year";
    public static final String CREATED_AT_COLUMN_NAME = "created_at";
    public static final String UPDATED_AT_COLUMN_NAME = "updated_at";

    public static final String USER_CATEGORY_MONTH_YEAR_UNIQUE_CONSTRAINT = "uk_user_category_month_year";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = USER_ID_COLUMN_NAME, nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = CATEGORY_ID_COLUMN_NAME,  nullable = false, updatable = false)
    private Category category;

    @Column(name = AMOUNT_LIMIT_COLUMN_NAME, nullable = false, precision = 19, scale = 4)
    private BigDecimal amountLimit;

    @Column(name = BUDGET_MONTH_COLUMN_NAME, nullable = false)
    private int budgetMonth;

    @Column(name = BUDGET_YEAR_COLUMN_NAME, nullable = false)
    private int budgetYear;

    @CreatedDate
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = UPDATED_AT_COLUMN_NAME, nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;
}
