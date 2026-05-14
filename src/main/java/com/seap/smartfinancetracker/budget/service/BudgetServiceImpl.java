package com.seap.smartfinancetracker.budget.service;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.mapper.BudgetMapper;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidParameterException;
import java.util.List;
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
    private final CategoryService categoryService;
    private final BudgetMapper budgetMapper;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Fetches the actual Category entity via {@link CategoryService} to ensure ownership.</li>
     * <li>Validates that the target category is of type {@code EXPENSE}.</li>
     * <li>Prevents creation if a budget already exists for this category in the specified month and year.</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional
    public BudgetResponse createBudget(UUID userId, BudgetCreateRequest budgetCreateRequest) {
        UUID categoryId = budgetCreateRequest.categoryId();

        Category category = categoryService.getCategoryEntity(userId, categoryId);

        if (category == null) {
            throw new InvalidParameterException("Category Not Found");
        }

        if (!(category.getTransactionType() == TransactionType.EXPENSE)) {
            throw new InvalidParameterException("Invalid Category Type To Create Budget");
        }

        Optional<Budget> optionalExistingBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(
                userId, budgetCreateRequest.categoryId(), budgetCreateRequest.month(), budgetCreateRequest.year());

        if (optionalExistingBudget.isPresent()) {
            Budget existingBudget = optionalExistingBudget.get();

            if (existingBudget.isActive()) {
                throw new IllegalArgumentException("An active budget already exists for this period.");
            }

            existingBudget.setActive(true);
            existingBudget.setAmountLimit(budgetCreateRequest.amountLimit());

            Budget reactivatedBudget = budgetRepository.save(existingBudget);
            return budgetMapper.toResponse(reactivatedBudget);
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
        Budget existingBudget = budgetRepository.findByUserIdAndId(userId, budgetId)
                .orElseThrow(() -> new IllegalArgumentException("Budget Not Found"));

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
                .map(budgetMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(UUID userId, UUID id) {
        Budget budget = budgetRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new IllegalArgumentException("Budget Not Found"));

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
        Budget existingBudget = budgetRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new IllegalArgumentException("Budget Not Found"));

        existingBudget.setActive(false);
        budgetRepository.save(existingBudget);
    }
}
