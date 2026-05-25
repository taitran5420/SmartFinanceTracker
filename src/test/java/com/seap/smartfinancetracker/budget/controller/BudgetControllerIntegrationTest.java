package com.seap.smartfinancetracker.budget.controller;

import com.seap.smartfinancetracker.budget.dto.BudgetCreateRequest;
import com.seap.smartfinancetracker.budget.dto.BudgetUpdateRequest;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnableJpaAuditing
@ActiveProfiles("test")
class BudgetControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;
    private Category testExpenseCategory;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create Test User
        testUser = Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create();
        testUser = userRepository.save(testUser);

        // Create Test Expense Category
        testExpenseCategory = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .set(field(Category::isActive), true)
                .create();
        testExpenseCategory = categoryRepository.save(testExpenseCategory);

        // Create JWT Test Token
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(testUser.getId())
                .email(testUser.getEmail())
                .build();
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }
    //</editor-fold>

    //<editor-fold desc="POST /budgets">
    @Test
    @DisplayName("Should create budget successfully")
    void shouldCreateBudgetSuccessfully() throws Exception {
        // Arrange
        BudgetCreateRequest request = new BudgetCreateRequest(
                testExpenseCategory.getId(),
                new BigDecimal("5000.00"),
                5,
                2026
        );

        // Act & Assert
        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountLimit").value(5000.00))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.categoryId").value(testExpenseCategory.getId().toString()));
    }

    @Test
    @DisplayName("Should fail to create budget when amount is negative")
    void shouldFailToCreateBudget_WhenAmountIsNegative() throws Exception {
        // Arrange
        BudgetCreateRequest request = new BudgetCreateRequest(
                testExpenseCategory.getId(),
                new BigDecimal("-100.00"),
                5,
                2026
        );

        // Act & Assert
        mockMvc.perform(post("/budgets")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    //</editor-fold>

    //<editor-fold desc="GET /budgets/{budgetId}">
    @Test
    @DisplayName("Should get budget by ID successfully")
    void shouldGetBudgetByIdSuccessfully() throws Exception {
        // Arrange
        Budget budget = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .set(field(Budget::getAmountLimit), new BigDecimal("3000.00"))
                .set(field(Budget::isActive), true)
                .create();
        budget = budgetRepository.save(budget);

        // Act & Assert
        mockMvc.perform(get("/budgets/{budgetId}", budget.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId().toString()))
                .andExpect(jsonPath("$.amountLimit").value(3000.00));
    }
    //</editor-fold>

    //<editor-fold desc="GET /budgets?categoryId={categoryId}">
    @Test
    @DisplayName("Should get all budgets by category ID")
    void shouldGetBudgetsByCategoryId() throws Exception {
        // Arrange
        Budget budget1 = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .create();
        Budget budget2 = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .create();

        budgetRepository.save(budget1);
        budgetRepository.save(budget2);

        // Act & Assert
        mockMvc.perform(get("/budgets")
                        .header("Authorization", "Bearer " + validToken)
                        .param("categoryId", testExpenseCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
    //</editor-fold>

    //<editor-fold desc="PUT /budgets/{budgetId}">
    @Test
    @DisplayName("Should update budget amount successfully")
    void shouldUpdateBudgetSuccessfully() throws Exception {
        // Arrange
        Budget budget = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .set(field(Budget::getAmountLimit), new BigDecimal("1000.00"))
                .set(field(Budget::isActive), true)
                .create();
        budget = budgetRepository.save(budget);

        BudgetUpdateRequest updateRequest = new BudgetUpdateRequest(new BigDecimal("9999.00"));

        // Act & Assert
        mockMvc.perform(put("/budgets/{budgetId}", budget.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountLimit").value(9999.00));
    }
    //</editor-fold>

    //<editor-fold desc="DELETE /budgets/{budgetId}">
    @Test
    @DisplayName("Should soft delete budget successfully")
    void shouldDeleteBudgetSuccessfully() throws Exception {
        // Arrange
        Budget budget = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .set(field(Budget::isActive), true)
                .create();
        budget = budgetRepository.save(budget);

        // Act & Assert
        mockMvc.perform(delete("/budgets/{budgetId}", budget.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        // Verify soft delete
        Budget deletedBudget = budgetRepository.findById(budget.getId()).orElseThrow();
        assertFalse(deletedBudget.isActive(), "Budget should be marked as inactive");
    }
    //</editor-fold>

    //<editor-fold desc="GET /budgets/{budgetId}/summary">
    @Test
    @DisplayName("Should get budget summary successfully")
    void shouldGetBudgetSummary() throws Exception {
        // Arrange
        Budget budget = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), testExpenseCategory)
                .set(field(Budget::getAmountLimit), new BigDecimal("2000.00"))
                .set(field(Budget::isActive), true)
                .create();
        budget = budgetRepository.save(budget);

        // Act & Assert
        mockMvc.perform(get("/budgets/{budgetId}/summary", budget.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountLimit").value(2000.00))
                .andExpect(jsonPath("$.spentAmount").value(0.00))
                .andExpect(jsonPath("$.remaining").value(2000.00))
                .andExpect(jsonPath("$.progressPercentage").value(0.0))
                .andExpect(jsonPath("$.isOverBudget").value(false));
    }
    //</editor-fold>
}