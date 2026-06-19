package com.seap.smartfinancetracker.analytics.controller;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link AnalyticsController}.
 * <p>
 * Drives the full HTTP → security → service → repository stack against a real Postgres
 * (Testcontainers), exercising the analytics aggregation queries end-to-end and verifying
 * the responses are correctly scoped to the authenticated user.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnalyticsControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserPrincipalMapper userPrincipalMapper;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;
    private Category rentCategory;
    private Category foodCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create());

        rentCategory = saveCategory("Rent", TransactionType.EXPENSE);
        foodCategory = saveCategory("Food", TransactionType.EXPENSE);
        salaryCategory = saveCategory("Salary", TransactionType.INCOME);

        UserPrincipal userPrincipal = userPrincipalMapper.toUserPrincipal(testUser);
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }

    private Category saveCategory(String name, TransactionType type) {
        return categoryRepository.save(Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getCategoryName), name)
                .set(field(Category::getTransactionType), type)
                .set(field(Category::isActive), true)
                .create());
    }

    private void saveTransaction(Category category, TransactionType type, String amount) {
        transactionRepository.save(Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), category)
                .set(field(Transaction::getTransactionType), type)
                .set(field(Transaction::getAmount), new BigDecimal(amount))
                .set(field(Transaction::isActive), true)
                .set(field(Transaction::getIdempotencyKey), UUID.randomUUID())
                .create());
    }
    //</editor-fold>

    //<editor-fold desc="GET /analytics/spending-by-category">
    @Test
    @DisplayName("Should return per-category spending with correct totals and percentage shares")
    void shouldReturnSpendingByCategory() throws Exception {
        // Arrange: Rent 75, Food 25 -> total 100 (75% / 25%); an income row must be ignored
        saveTransaction(rentCategory, TransactionType.EXPENSE, "75.00");
        saveTransaction(foodCategory, TransactionType.EXPENSE, "25.00");
        saveTransaction(salaryCategory, TransactionType.INCOME, "500.00");

        // Act & Assert: ordered by descending spend, so Rent comes first
        mockMvc.perform(get("/analytics/spending-by-category")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(100.00))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].categoryName").value("Rent"))
                .andExpect(jsonPath("$.categories[0].percentage").value(75.0))
                .andExpect(jsonPath("$.categories[1].categoryName").value("Food"))
                .andExpect(jsonPath("$.categories[1].percentage").value(25.0));
    }

    @Test
    @DisplayName("Should return zero total and an empty list when the user has no expenses")
    void shouldReturnEmptySpendingByCategory_WhenNoExpenses() throws Exception {
        // Arrange: only income exists
        saveTransaction(salaryCategory, TransactionType.INCOME, "500.00");

        // Act & Assert
        mockMvc.perform(get("/analytics/spending-by-category")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.categories", hasSize(0)));
    }
    //</editor-fold>

    //<editor-fold desc="GET /analytics/summary">
    @Test
    @DisplayName("Should return income, expense, net, count and the top spending category")
    void shouldReturnPeriodSummary() throws Exception {
        // Arrange: income 200; expenses Rent 75 + Food 25 = 100 -> net 100, count 3, top = Rent
        saveTransaction(salaryCategory, TransactionType.INCOME, "200.00");
        saveTransaction(rentCategory, TransactionType.EXPENSE, "75.00");
        saveTransaction(foodCategory, TransactionType.EXPENSE, "25.00");

        // Act & Assert
        mockMvc.perform(get("/analytics/summary")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(200.00))
                .andExpect(jsonPath("$.totalExpense").value(100.00))
                .andExpect(jsonPath("$.net").value(100.00))
                .andExpect(jsonPath("$.transactionCount").value(3))
                .andExpect(jsonPath("$.topCategoryName").value("Rent"))
                .andExpect(jsonPath("$.topCategoryAmount").value(75.00));
    }
    //</editor-fold>

    //<editor-fold desc="GET /analytics/income-expense-trend">
    @Test
    @DisplayName("Should return one merged trend point for the current month")
    void shouldReturnIncomeExpenseTrend() throws Exception {
        // Arrange: all transactions are created "now"; EXTRACT runs in UTC, so derive expectations in UTC
        saveTransaction(salaryCategory, TransactionType.INCOME, "200.00");
        saveTransaction(rentCategory, TransactionType.EXPENSE, "80.00");

        ZonedDateTime nowUtc = Instant.now().atZone(ZoneOffset.UTC);

        // Act & Assert
        mockMvc.perform(get("/analytics/income-expense-trend")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].year").value(nowUtc.getYear()))
                .andExpect(jsonPath("$[0].month").value(nowUtc.getMonthValue()))
                .andExpect(jsonPath("$[0].totalIncome").value(200.00))
                .andExpect(jsonPath("$[0].totalExpense").value(80.00))
                .andExpect(jsonPath("$[0].net").value(120.00));
    }
    //</editor-fold>

    //<editor-fold desc="Validation & Security">
    @Test
    @DisplayName("Should reject an inverted date range with 400 Bad Request")
    void shouldRejectInvertedDateRange() throws Exception {
        // Act & Assert: startDate after endDate
        mockMvc.perform(get("/analytics/summary")
                        .header("Authorization", "Bearer " + validToken)
                        .param("startDate", "2026-06-10T00:00:00Z")
                        .param("endDate", "2026-06-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ANL-400-01"));
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        // Act & Assert: no Authorization header
        mockMvc.perform(get("/analytics/summary"))
                .andExpect(status().is4xxClientError());
    }
    //</editor-fold>
}
