package com.seap.smartfinancetracker.budget.entity;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing a financial budget in the database.
 */
@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_category_month_year",
                columnNames = {"user_id", "category_id", "budget_month", "budget_year"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Budget {

    private static final String USER_ID_COLUMN_NAME = "user_id";
    private static final String CATEGORY_ID_COLUMN_NAME = "category_id";
    private static final String AMOUNT_LIMIT_COLUMN_NAME = "amount_limit";
    private static final String BUDGET_MONTH_COLUMN_NAME = "budget_month";
    private static final String BUDGET_YEAR_COLUMN_NAME = "budget_year";
    private static final String CREATED_AT_COLUMN_NAME = "created_at";
    private static final String UPDATED_AT_COLUMN_NAME = "updated_at";

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

    @Builder.Default
    @Column(name = CREATED_AT_COLUMN_NAME, nullable = false, updatable = false)
    private Instant createdAt =  Instant.now();

    @UpdateTimestamp
    @Column(name = UPDATED_AT_COLUMN_NAME, nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;
}
