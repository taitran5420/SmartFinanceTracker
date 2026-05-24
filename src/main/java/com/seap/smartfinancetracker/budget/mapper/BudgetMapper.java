package com.seap.smartfinancetracker.budget.mapper;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetSummaryResponse;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Component responsible for mapping between {@link Budget} entities
 * and their corresponding Data Transfer Objects (DTOs).
 *
 * <p>This class handles:
 * <ul>
 * <li>Mapping {@link BudgetCreateRequest} to a new {@link Budget} entity</li>
 * <li>Mapping a persisted {@link Budget} to a standard {@link BudgetResponse}</li>
 * <li>Aggregating a {@link Budget} and dynamically calculated transaction metrics into a {@link BudgetSummaryResponse}</li>
 * </ul>
 */
@Component
public class BudgetMapper {
    /**
     * Converts a budget creation request into a {@link Budget} entity.
     *
     * @param userId              the unique identifier of the user creating the budget
     * @param budgetCreateRequest the payload containing budget details
     * @return a new {@link Budget} entity ready to be persisted, or {@code null} if the request is null
     */
    public Budget toEntity(UUID userId, BudgetCreateRequest budgetCreateRequest) throws NullPointerException {

        return Budget.builder()
                .user(User.builder().id(userId).build())
                .category(Category.builder().id(budgetCreateRequest.categoryId()).build())
                .amountLimit(budgetCreateRequest.amountLimit())
                .budgetMonth(budgetCreateRequest.month())
                .budgetYear(budgetCreateRequest.year())
                .active(true)
                .build();
    }

    /**
     * Converts a persisted {@link Budget} entity into a {@link BudgetResponse} DTO.
     *
     * @param budget the budget entity retrieved from the database
     * @return the mapped response DTO, or {@code null} if the entity is null
     */
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
                .updatedAt(budget.getUpdatedAt())
                .build();
    }

    /**
     * Aggregates a budget entity and its calculated transaction metrics into a comprehensive summary.
     * <p>
     * This method acts as a data aggregator (following the Backend-For-Frontend pattern).
     * It combines static budget limits from the database with dynamic spending calculations
     * provided by the service layer, while also denormalizing the category name for immediate UI display.
     * </p>
     *
     * @param budget             the core budget entity
     * @param spentAmount        the total monetary amount already spent in this category
     * @param remainingAmount    the calculated remaining budget (can be negative if overspent)
     * @param progressPercentage the completion rate of the budget as a percentage
     * @param isOverBudget       flag indicating if the spending has strictly exceeded the limit
     * @return the fully populated {@link BudgetSummaryResponse}, or {@code null} if the budget is null
     */
    public BudgetSummaryResponse toBudgetSummaryResponse(
            Budget budget,
            BigDecimal spentAmount,
            BigDecimal remainingAmount,
            double progressPercentage,
            boolean isOverBudget) {

        if (budget == null) {
            return null;
        }

        return BudgetSummaryResponse.builder()
                .budgetId(budget.getId())
                .categoryId(budget.getCategory().getId())
                .categoryName(budget.getCategory().getCategoryName())
                .amountLimit(budget.getAmountLimit())
                .spentAmount(spentAmount)
                .remaining(remainingAmount)
                .progressPercentage(progressPercentage)
                .isOverBudget(isOverBudget)
                .build();
    }
}
