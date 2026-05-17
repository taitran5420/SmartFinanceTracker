package com.seap.smartfinancetracker.budget.controller;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetSummaryResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;
import com.seap.smartfinancetracker.budget.service.BudgetService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing financial budgets.
 * <p>
 * Exposes endpoints for clients to perform CRUD operations on their budgets.
 * All endpoints are strictly secured and isolated using the {@link CurrentUserId}
 * annotation, ensuring that users can only interact with their own financial data.
 * </p>
 */
@RestController
@RequestMapping("budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Creates a new budget for a specific expense category.
     *
     * @param userId              the authenticated user's ID, automatically injected from the security context
     * @param budgetCreateRequest the payload containing category, amount limit, month, and year
     * @return a {@link ResponseEntity} containing the created {@link BudgetResponse} and a 201 CREATED status
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @CurrentUserId UUID userId,
            @Valid @RequestBody BudgetCreateRequest budgetCreateRequest) {
        BudgetResponse budgetResponse = budgetService.createBudget(userId, budgetCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetResponse);
    }

    /**
     * Retrieves a specific budget by its unique identifier.
     *
     * @param userId   the authenticated user's ID
     * @param budgetId the unique identifier of the budget to fetch
     * @return a {@link ResponseEntity} containing the requested {@link BudgetResponse}
     */
    @GetMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> getBudgetById(@CurrentUserId UUID userId, @PathVariable UUID budgetId) {
        BudgetResponse budgetResponse = budgetService.getBudgetById(userId, budgetId);
        return ResponseEntity.ok(budgetResponse);
    }

    /**
     * Retrieves all budgets associated with a specific category for the authenticated user.
     * <p>
     * Typically used by the frontend to display historical budgeting trends for a single category.
     * </p>
     *
     * @param userId     the authenticated user's ID
     * @param categoryId the unique identifier of the category to filter by
     * @return a {@link ResponseEntity} containing a list of {@link BudgetResponse} matching the criteria
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @CurrentUserId UUID userId,
            @RequestParam(name = "categoryId") UUID categoryId) {
        List<BudgetResponse> budgetResponses = budgetService.findAllBudgetsWithCategory(userId, categoryId);

        return ResponseEntity.ok(budgetResponses);
    }

    /**
     * Updates the spending limit of an existing budget.
     *
     * @param userId              the authenticated user's ID
     * @param budgetId            the unique identifier of the budget to update
     * @param budgetUpdateRequest the payload containing the new positive amount limit
     * @return a {@link ResponseEntity} containing the updated {@link BudgetResponse}
     */
    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @CurrentUserId UUID userId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody BudgetUpdateRequest budgetUpdateRequest){
        BudgetResponse updateBudgetResponse = budgetService.updateBudget(userId, budgetId, budgetUpdateRequest);

        return ResponseEntity.ok(updateBudgetResponse);
    }

    /**
     * Soft-deletes a specific budget.
     *
     * @param userId   the authenticated user's ID
     * @param budgetId the unique identifier of the budget to delete
     * @return a {@link ResponseEntity} with a 204 NO CONTENT status upon successful deletion
     */
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deleteBudget(@CurrentUserId UUID userId, @PathVariable UUID budgetId) {
        budgetService.deleteBudget(userId, budgetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a comprehensive summary for a specific budget.
     * <p>
     * This endpoint returns aggregated data, combining the static budget limits with
     * dynamically calculated transaction metrics. It is primarily designed to populate dashboard and reporting views.
     * </p>
     *
     * @param userId   the authenticated user's ID
     * @param budgetId the unique identifier of the budget to summarize
     * @return a {@link ResponseEntity} containing the calculated {@link BudgetSummaryResponse}
     */
    @GetMapping("/{budgetId}/summary")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(@CurrentUserId UUID userId, @PathVariable UUID budgetId) {
        BudgetSummaryResponse budgetSummaryResponse = budgetService.getBudgetSummary(userId, budgetId);
        return ResponseEntity.ok(budgetSummaryResponse);
    }
}
