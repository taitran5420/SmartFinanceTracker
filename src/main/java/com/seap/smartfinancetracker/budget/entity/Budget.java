package com.seap.smartfinancetracker.budget.entity;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id",  nullable = false, updatable = false)
    private Category category;

    @Column(name = "amount_limit", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountLimit;

    @Column(name = "budget_month", nullable = false)
    private int budgetMonth;

    @Column(name = "budget_year", nullable = false)
    private int budgetYear;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt =  Instant.now();

    @Column(nullable = false)
    private boolean active = true;
}
