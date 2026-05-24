package com.seap.smartfinancetracker.budget.service;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetSummaryResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.exception.BudgetErrorCode;
import com.seap.smartfinancetracker.budget.mapper.BudgetMapper;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.exception.CategoryErrorCode;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link BudgetService}.
 * <p>
 * Orchestrates the business rules for budgeting, including enforcing category ownership,
 * preventing duplicate budgets for the same period, and ensuring budgets are only
 * applied to EXPENSE categories.
 * </p>
 */
@Service
@AllArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final BudgetMapper budgetMapper;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Fetches the actual Category entity via {@link CategoryService} to ensure ownership.</li>
     * <li>Validates that the target category is strictly of type {@code EXPENSE}.</li>
     * <li>Prevents creation if an <b>active</b> budget already exists for this category in the specified month and year.</li>
     * <li><b>Reactivation Mechanism:</b> If a previously soft-deleted (inactive) budget is found for the same
     * category and period, it gracefully reactivates the existing record and applies the new amount limit
     * instead of creating a duplicate database row.</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public BudgetResponse createBudget(UUID userId, BudgetCreateRequest budgetCreateRequest) {
        UUID categoryId = budgetCreateRequest.categoryId();

        validateCategoryForBudget(userId, categoryId);

        Optional<Budget> optionalExistingBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(
                userId, budgetCreateRequest.categoryId(), budgetCreateRequest.month(), budgetCreateRequest.year());

        if (optionalExistingBudget.isPresent()) {
            Budget existingBudget = optionalExistingBudget.get();

            return reactivateExistingBudget(existingBudget, budgetCreateRequest.amountLimit());
        }

        Budget budget = budgetMapper.toEntity(userId, budgetCreateRequest);
        Budget createdBudget = budgetRepository.save(budget);

        return budgetMapper.toResponse(createdBudget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID userId, UUID budgetId, BudgetUpdateRequest budgetUpdateRequest) {
        Budget existingBudget = getBudgetOrThrow(userId, budgetId);

        existingBudget.setAmountLimit(budgetUpdateRequest.amountLimit());

        Budget updatedBudget = budgetRepository.save(existingBudget);

        return budgetMapper.toResponse(updatedBudget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BudgetResponse> findAllBudgetsWithCategory(UUID userId, UUID categoryId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndCategoryId(userId, categoryId);

        return budgets.stream()
                .filter(Objects::nonNull)
                .map(budgetMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(UUID userId, UUID id) {
        Budget budget = getBudgetOrThrow(userId, id);

        return budgetMapper.toResponse(budget);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Performs a soft-delete by setting the {@code active} flag to false.
     * </p>
     */
    @Override
    @Transactional
    public void deleteBudget(UUID userId, UUID id) {
        Budget existingBudget = getBudgetOrThrow(userId, id);

        existingBudget.setActive(false);
        budgetRepository.save(existingBudget);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Use {@link TransactionRepository} to calculate total spent.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)

    public BudgetSummaryResponse getBudgetSummary(UUID userId, UUID id) {
        Budget budget = budgetRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.BUDGET_NOT_FOUND));

        if (!budget.isActive()) {
           throw new BusinessException(BudgetErrorCode.BUDGET_NOT_ACTIVE);
        }

        BigDecimal budgetSpent = transactionRepository.calculateTotalSpentByCategoryAndMonth(
                userId,
                budget.getCategory().getId(),
                budget.getBudgetMonth(),
                budget.getBudgetYear()
        );

        return calculateAndMapToSummary(budget, budgetSpent);
    }

    private Budget getBudgetOrThrow(UUID userId, UUID id) {
        return budgetRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new BusinessException(BudgetErrorCode.BUDGET_NOT_FOUND));
    }

    private void validateCategoryForBudget(UUID userId, UUID categoryId) {
        Category category = categoryService.getCategoryEntity(userId, categoryId);

        if (category == null || !category.isActive()) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        if (!(category.getTransactionType() == TransactionType.EXPENSE)) {
            throw new BusinessException(BudgetErrorCode.CATEGORY_MUST_BE_EXPENSE);
        }
    }

    private BudgetResponse reactivateExistingBudget(Budget existingBudget, BigDecimal newAmountLimit) {
        if (existingBudget.isActive()) {
            throw new BusinessException(BudgetErrorCode.ACTIVE_BUDGET_EXISTS);
        }
        existingBudget.setActive(true);
        existingBudget.setAmountLimit(newAmountLimit);
        return budgetMapper.toResponse(budgetRepository.save(existingBudget));
    }

    private BudgetSummaryResponse calculateAndMapToSummary(Budget budget, BigDecimal budgetSpent) {
        BigDecimal budgetLimit = budget.getAmountLimit();
        BigDecimal budgetRemaining = budgetLimit.subtract(budgetSpent);

        BigDecimal percentageSpent = budgetLimit.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO :
                budgetSpent.divide(budgetLimit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        boolean isOverBudget = budgetSpent.compareTo(budgetLimit) > 0;

        return budgetMapper.toBudgetSummaryResponse(
                budget, budgetSpent, budgetRemaining, percentageSpent.doubleValue(), isOverBudget
        );
    }
}
