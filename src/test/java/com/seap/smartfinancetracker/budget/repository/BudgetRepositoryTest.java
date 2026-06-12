package com.seap.smartfinancetracker.budget.repository;

import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BudgetRepositoryTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BudgetRepository budgetRepository;
    //</editor-fold>

    //<editor-fold desc="Test findByUserIdAndId">
    @Test
    @DisplayName("Should return budget belong to user")
    void findByUserIdAndId_ShouldReturnBudget() {
        // Arrange
        User targetUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        targetUser = entityManager.persistAndFlush(targetUser);

        Category targetUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create();
        targetUserCategory = entityManager.persistAndFlush(targetUserCategory);

        Budget targetBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::getUser), targetUser)
                .set(Select.field(Budget::getCategory), targetUserCategory)
                .ignore(Select.field(Budget::getId))
                .create();

        targetBudget = entityManager.persistAndFlush(targetBudget);

        Optional<Budget> resultBudget = budgetRepository.findByUserIdAndId(targetUser.getId(), targetBudget.getId());

        assertFalse(resultBudget.isEmpty(), "Budget should not be empty");
        assertEquals(targetUserCategory.getId(), resultBudget.get().getCategory().getId(), "Budget category should be the target user category");
        assertEquals(targetUser.getId(), resultBudget.get().getUser().getId(), "Budget user should be the target user");

    }
    //</editor-fold>

    //<editor-fold desc="Test findByUserIdAndCategoryId">
    @Test
    @DisplayName("Should return budget belong to user and category")
    void findByUserIdAndCategoryId_ShouldReturnBudget() {
        // Arrange
        User targetUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        targetUser = entityManager.persistAndFlush(targetUser);

        Category targetUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create();
        targetUserCategory = entityManager.persistAndFlush(targetUserCategory);

        Category otherUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create();
        otherUserCategory = entityManager.persistAndFlush(otherUserCategory);

        Budget targetBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::getUser), targetUser)
                .set(Select.field(Budget::getCategory), targetUserCategory)
                .ignore(Select.field(Budget::getId))
                .create();
        Budget otherBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::getUser), targetUser)
                .set(Select.field(Budget::getCategory), otherUserCategory)
                .ignore(Select.field(Budget::getId))
                .create();

        budgetRepository.saveAll(List.of(targetBudget, otherBudget));

        // Act
        List<Budget> budgets = budgetRepository.findByUserIdAndCategoryId(targetUser.getId(), targetUserCategory.getId());

        // Assert
        assertEquals(1, budgets.size(), "Should find exactly one budget for target user and category");
        assertEquals(targetUser.getId(), budgets.getFirst().getUser().getId(), "Return budget must belong to the user");
        assertEquals(targetUserCategory.getId(), budgets.getFirst().getCategory().getId(), "Return budget must belong to the category");
    }
    //</editor-fold>

    //<editor-fold desc="Test findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear">
    @Test
    @DisplayName("Should return budget for specific user, category, month, and year")
    void findByUserIdAndCategoryIdAndMonthAndYear_ShouldReturnBudget() {
        // Arrange
        User targetUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        targetUser = entityManager.persistAndFlush(targetUser);

        Category targetUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create();
        targetUserCategory = entityManager.persistAndFlush(targetUserCategory);

        int targetMonth = 5;
        int targetYear = 2026;

        Budget targetBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::getUser), targetUser)
                .set(Select.field(Budget::getCategory), targetUserCategory)
                .set(Select.field(Budget::getBudgetMonth), targetMonth)
                .set(Select.field(Budget::getBudgetYear), targetYear)
                .ignore(Select.field(Budget::getId))
                .create();
        targetBudget = entityManager.persistAndFlush(targetBudget);

        // Act
        Optional<Budget> resultBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(
                targetUser.getId(), targetUserCategory.getId(), targetMonth, targetYear);

        // Assert
        assertFalse(resultBudget.isEmpty(), "Budget should not be empty");
        assertEquals(targetBudget.getId(), resultBudget.get().getId(), "Budget ID should match the target budget");
        assertEquals(targetMonth, resultBudget.get().getBudgetMonth(), "Budget month should match");
        assertEquals(targetYear, resultBudget.get().getBudgetYear(), "Budget year should match");
    }

    @Test
    @DisplayName("Should return empty when budget for specific user, category, month, and year does not exist")
    void findByUserIdAndCategoryIdAndMonthAndYear_ShouldReturnEmpty() {
        // Arrange
        User targetUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        targetUser = entityManager.persistAndFlush(targetUser);

        Category targetUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create();
        targetUserCategory = entityManager.persistAndFlush(targetUserCategory);

        int nonExistentMonth = 1;
        int nonExistentYear = 2026;

        // Act
        Optional<Budget> resultBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(
                targetUser.getId(), targetUserCategory.getId(), nonExistentMonth, nonExistentYear);

        // Assert
        assertTrue(resultBudget.isEmpty(), "Budget should be empty because it was never created");
    }
    //</editor-fold>
}
