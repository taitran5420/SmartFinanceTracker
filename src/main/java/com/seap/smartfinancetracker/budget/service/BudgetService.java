package com.seap.smartfinancetracker.budget.service;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business logic contract for managing financial budgets.
 * <p>
 * This service provides operations for creating, retrieving, updating, and deleting
 * budgets. Implementations must strictly enforce data isolation by verifying the
 * {@code userId} for all operations to ensure users can only access and modify their own budgets.
 * </p>
 */
public interface BudgetService {
    /**
     * Creates a new financial budget for a specific user.
     *
     * @param userId              the unique identifier of the user creating the budget
     * @param budgetCreateRequest the payload containing budget details (category, amount, month, year)
     * @return a {@link BudgetResponse} representing the newly created budget
     */
    BudgetResponse createBudget(UUID userId, BudgetCreateRequest budgetCreateRequest);

    /**
     * Updates the spending limit of an existing budget.
     * <p>
     * Note: Core attributes such as the associated category and timeframe (month/year)
     * cannot be modified after creation.
     * </p>
     *
     * @param userId              the unique identifier of the user requesting the update
     * @param budgetId            the unique identifier of the budget to be updated
     * @param budgetUpdateRequest the payload containing the new spending limit
     * @return a {@link BudgetResponse} representing the updated budget
     */
    BudgetResponse updateBudget(UUID userId, UUID budgetId, BudgetUpdateRequest budgetUpdateRequest);

    /**
     * Retrieves all budgets associated with a specific user and category.
     * <p>
     * This is useful for fetching the historical budgeting trend for a particular
     * category over multiple months or years.
     * </p>
     *
     * @param userId     the unique identifier of the user
     * @param categoryId the unique identifier of the category
     * @return a list of {@link BudgetResponse} objects matching the criteria
     */
    List<BudgetResponse> findAllBudgetsWithCategory(UUID userId, UUID categoryId);

    /**
     * Retrieves a specific budget by its unique identifier.
     *
     * @param userId the unique identifier of the user requesting the budget
     * @param id     the unique identifier of the budget to retrieve
     * @return a {@link BudgetResponse} representing the requested budget
     */
    BudgetResponse getBudgetById(UUID userId, UUID id);

    /**
     * Deletes a specific budget belonging to the specified user.
     * <p>
     * Implementations typically perform a soft delete to maintain historical integrity.
     * </p>
     *
     * @param userId the unique identifier of the user requesting the deletion
     * @param id     the unique identifier of the budget to delete
     */
    void deleteBudget(UUID userId, UUID id);
}
