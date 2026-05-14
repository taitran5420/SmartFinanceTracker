package com.seap.smartfinancetracker.budget.mapper;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Component responsible for mapping between {@link Budget} entities
 * and their corresponding Data Transfer Objects (DTOs).
 *
 * <p>This class handles:
 * <ul>
 *     <li>Mapping {@link BudgetCreateRequest} to {@link Budget}</li>
 *     <li>Mapping {@link Budget} to {@link BudgetResponse}</li>
 * </ul>
 *
 */
@Component
public class BudgetMapper {
    public Budget toEntity(UUID userId, BudgetCreateRequest budgetCreateRequest) {
        if (budgetCreateRequest == null)
            return null;

        return Budget.builder()
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(budgetCreateRequest.categoryId()).build())
                .amountLimit(budgetCreateRequest.amountLimit())
                .budgetMonth(budgetCreateRequest.month())
                .budgetYear(budgetCreateRequest.year())
                .active(true)
                .build();
    }

    public BudgetResponse toResponse(Budget budget) {
        if (budget == null)
            return null;

        return BudgetResponse.builder()
                .id(budget.getId())
                .categoryId(budget.getCategory().getId())
                .amountLimit(budget.getAmountLimit())
                .month(budget.getBudgetMonth())
                .year(budget.getBudgetYear())
                .active(budget.isActive())
                .createdAt(budget.getCreatedAt())
                .build();
    }
}
