package com.seap.smartfinancetracker.budget.service;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetSummaryResponse;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.mapper.BudgetMapper;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BudgetServiceImplTest {

    //<editor-fold desc="Setup & Configurations">
    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetServiceImpl budgetService;
    //</editor-fold>

    //<editor-fold desc="Test createBudget">
    @Test
    @DisplayName("Should successfully create a new budget when no existing budget found")
    void createBudget_ShouldReturnResponse_WhenNewBudget() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetCreateRequest request = new BudgetCreateRequest(categoryId, BigDecimal.valueOf(5000), 5, 2026);

        Category category = mock(Category.class);
        when(category.isActive()).thenReturn(true);
        when(category.getTransactionType()).thenReturn(TransactionType.EXPENSE);

        Budget mappedEntity = Instancio.create(Budget.class);
        Budget savedEntity = Instancio.create(Budget.class);
        BudgetResponse expectedResponse = Instancio.create(BudgetResponse.class);

        when(categoryService.getCategoryEntity(userId, categoryId)).thenReturn(category);
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(userId, categoryId, request.month(), request.year()))
                .thenReturn(Optional.empty());
        when(budgetMapper.toEntity(userId, request)).thenReturn(mappedEntity);
        when(budgetRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(budgetMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // Act
        BudgetResponse actualResponse = budgetService.createBudget(userId, request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(budgetRepository, times(1)).save(mappedEntity);
    }

    @Test
    @DisplayName("Should reactivate and update existing budget if it is inactive")
    void createBudget_ShouldReactivate_WhenInactiveBudgetExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetCreateRequest request = new BudgetCreateRequest(categoryId, BigDecimal.valueOf(8000), 5, 2026);

        Category category = mock(Category.class);
        when(category.isActive()).thenReturn(true);
        when(category.getTransactionType()).thenReturn(TransactionType.EXPENSE);

        Budget existingInactiveBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), false)
                .create();
        BudgetResponse expectedResponse = Instancio.create(BudgetResponse.class);

        when(categoryService.getCategoryEntity(userId, categoryId)).thenReturn(category);
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(userId, categoryId, request.month(), request.year()))
                .thenReturn(Optional.of(existingInactiveBudget));
        when(budgetRepository.save(existingInactiveBudget)).thenReturn(existingInactiveBudget);
        when(budgetMapper.toResponse(existingInactiveBudget)).thenReturn(expectedResponse);

        // Act
        BudgetResponse actualResponse = budgetService.createBudget(userId, request);

        // Assert
        assertNotNull(actualResponse);
        assertTrue(existingInactiveBudget.isActive(), "Budget should be reactivated");
        assertEquals(request.amountLimit(), existingInactiveBudget.getAmountLimit(), "Budget limit should be updated");
        verify(budgetRepository, times(1)).save(existingInactiveBudget);
        verify(budgetMapper, never()).toEntity(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when active budget already exists for the period")
    void createBudget_ShouldThrowException_WhenActiveBudgetExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetCreateRequest request = new BudgetCreateRequest(categoryId, BigDecimal.valueOf(5000), 5, 2026);

        Category category = mock(Category.class);
        when(category.isActive()).thenReturn(true);
        when(category.getTransactionType()).thenReturn(TransactionType.EXPENSE);

        Budget activeBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), true)
                .create();

        when(categoryService.getCategoryEntity(userId, categoryId)).thenReturn(category);
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(userId, categoryId, request.month(), request.year()))
                .thenReturn(Optional.of(activeBudget));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.createBudget(userId, request)
        );
        assertEquals("An active budget already exists for this period.", exception.getMessage());
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    @DisplayName("Should throw exception when category is not EXPENSE")
    void createBudget_ShouldThrowException_WhenCategoryIsNotExpense() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        BudgetCreateRequest request = new BudgetCreateRequest(categoryId, BigDecimal.valueOf(5000), 5, 2026);

        Category category = mock(Category.class);
        when(category.isActive()).thenReturn(true);
        when(category.getTransactionType()).thenReturn(TransactionType.INCOME);

        when(categoryService.getCategoryEntity(userId, categoryId)).thenReturn(category);

        // Act & Assert
        InvalidParameterException exception = assertThrows(
                InvalidParameterException.class,
                () -> budgetService.createBudget(userId, request)
        );
        assertEquals("Invalid Category Type To Create Budget", exception.getMessage());
    }
    //</editor-fold>

    //<editor-fold desc="Test updateBudget">
    @Test
    @DisplayName("Should successfully update an existing budget")
    void updateBudget_ShouldUpdateAndReturnResponse_WhenBudgetExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        BudgetUpdateRequest updateRequest = new BudgetUpdateRequest(BigDecimal.valueOf(10000));

        Budget existingBudget = Instancio.create(Budget.class);
        BudgetResponse expectedResponse = Instancio.create(BudgetResponse.class);

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);
        when(budgetMapper.toResponse(existingBudget)).thenReturn(expectedResponse);

        // Act
        BudgetResponse actualResponse = budgetService.updateBudget(userId, budgetId, updateRequest);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(updateRequest.amountLimit(), existingBudget.getAmountLimit());
        assertEquals(expectedResponse, actualResponse);
        verify(budgetRepository, times(1)).save(existingBudget);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent budget")
    void updateBudget_ShouldThrowException_WhenBudgetNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        BudgetUpdateRequest updateRequest = new BudgetUpdateRequest(BigDecimal.valueOf(10000));

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.updateBudget(userId, budgetId, updateRequest)
        );
        verify(budgetRepository, never()).save(any());
    }
    //</editor-fold>

    //<editor-fold desc="Test findAllBudgetsWithCategory">
    @Test
    @DisplayName("Should return list of budgets for a specific category")
    void findAllBudgetsWithCategory_ShouldReturnMappedList() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Budget budget1 = Instancio.create(Budget.class);
        Budget budget2 = Instancio.create(Budget.class);
        BudgetResponse response1 = Instancio.create(BudgetResponse.class);
        BudgetResponse response2 = Instancio.create(BudgetResponse.class);

        when(budgetRepository.findByUserIdAndCategoryId(userId, categoryId)).thenReturn(List.of(budget1, budget2));
        when(budgetMapper.toResponse(budget1)).thenReturn(response1);
        when(budgetMapper.toResponse(budget2)).thenReturn(response2);

        // Act
        List<BudgetResponse> results = budgetService.findAllBudgetsWithCategory(userId, categoryId);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.contains(response1));
        assertTrue(results.contains(response2));
    }
    //</editor-fold>

    //<editor-fold desc="Test getBudgetById">
    @Test
    @DisplayName("Should return budget response when budget exists")
    void getBudgetById_ShouldReturnResponse() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Instancio.create(Budget.class);
        BudgetResponse expectedResponse = Instancio.create(BudgetResponse.class);

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.of(budget));
        when(budgetMapper.toResponse(budget)).thenReturn(expectedResponse);

        // Act
        BudgetResponse actualResponse = budgetService.getBudgetById(userId, budgetId);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }
    //</editor-fold>

    //<editor-fold desc="Test deleteBudget">
    @Test
    @DisplayName("Should soft delete budget by setting active to false")
    void deleteBudget_ShouldSetActiveToFalse() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget existingBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), true)
                .create();

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.of(existingBudget));

        // Act
        budgetService.deleteBudget(userId, budgetId);

        // Assert
        assertFalse(existingBudget.isActive(), "Budget should be marked as inactive");
        verify(budgetRepository, times(1)).save(existingBudget);
    }
    //</editor-fold>

    //<editor-fold desc="Test getBudgetSummary">
    @Test
    @DisplayName("Should return budget summary successfully")
    void getBudgetSummary_ShouldReturnSummary_WhenBudgetIsActive() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Instancio.of(Category.class)
                .set(Select.field(Category::getId), categoryId)
                .create();

        Budget budget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), true)
                .set(Select.field(Budget::getCategory), category)
                .set(Select.field(Budget::getAmountLimit), BigDecimal.valueOf(1000))
                .create();

        BigDecimal spentAmount = BigDecimal.valueOf(250);
        BudgetSummaryResponse expectedSummary = Instancio.create(BudgetSummaryResponse.class);

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.of(budget));
        when(transactionRepository.calculateTotalSpentByCategoryAndMonth(
                userId, categoryId, budget.getBudgetMonth(), budget.getBudgetYear()
        )).thenReturn(spentAmount);

        when(budgetMapper.toBudgetSummaryResponse(
                eq(budget), eq(spentAmount), eq(BigDecimal.valueOf(750)), eq(25.0), eq(false)
        )).thenReturn(expectedSummary);

        // Act
        BudgetSummaryResponse actualSummary = budgetService.getBudgetSummary(userId, budgetId);

        // Assert
        assertNotNull(actualSummary);
        assertEquals(expectedSummary, actualSummary);
    }

    @Test
    @DisplayName("Should throw exception when calculating summary for inactive budget")
    void getBudgetSummary_ShouldThrowException_WhenBudgetNotActive() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget inactiveBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), false)
                .create();

        when(budgetRepository.findByUserIdAndId(userId, budgetId)).thenReturn(Optional.of(inactiveBudget));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> budgetService.getBudgetSummary(userId, budgetId)
        );
        assertEquals("Budget Not Active", exception.getMessage());
        verify(transactionRepository, never()).calculateTotalSpentByCategoryAndMonth(any(), any(), anyInt(), anyInt());
    }
    //</editor-fold>`
}