package com.seap.smartfinancetracker.budget.repository;

import com.seap.smartfinancetracker.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Optional<Budget> findByUserIdAndId(UUID userId, UUID id);

    List<Budget> findByUserIdAndCategoryId(UUID userId, UUID categoryId);

    Optional<Budget> findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(UUID userId, UUID categoryId, Integer month, Integer year);
}
