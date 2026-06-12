package com.seap.smartfinancetracker.transaction.controller;

import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.TransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

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
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserPrincipalMapper userPrincipalMapper;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;
    private Category testCategoryExpense;
    private Category testCategoryIncome;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create Test User
        testUser = Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create();
        testUser = userRepository.save(testUser);

        // Create Test Category to satisfy FK constraints
        testCategoryExpense = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .set(field(Category::isActive), true)
                .create();
        testCategoryExpense = categoryRepository.save(testCategoryExpense);

        testCategoryIncome = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getTransactionType), TransactionType.INCOME)
                .set(field(Category::isActive), true)
                .create();
        testCategoryIncome = categoryRepository.save(testCategoryIncome);

        // Create JWT Test Token
        UserPrincipal userPrincipal = userPrincipalMapper.toUserPrincipal(testUser);
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }
    //</editor-fold>

    //<editor-fold desc="POST /transactions">
    @Test
    void shouldCreateTransactionSuccessfully() throws Exception {
        // Arrange
        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(field(TransactionCreateRequest::categoryId), testCategoryExpense.getId())
                .set(field(TransactionCreateRequest::transactionType), TransactionType.EXPENSE)
                .set(field(TransactionCreateRequest::amount), new BigDecimal("150.00"))
                .create();

        // Act & Assert
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.transactionType").value(TransactionType.EXPENSE.name()));
    }

    @Test
    void shouldFailToCreateTransaction_WhenNoTokenProvided() throws Exception {
        TransactionCreateRequest request = Instancio.create(TransactionCreateRequest.class);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFailToCreateTransaction_WhenAmountOverdraftLimit() throws Exception {
        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(field(TransactionCreateRequest::categoryId), testCategoryExpense.getId())
                .set(field(TransactionCreateRequest::transactionType), TransactionType.EXPENSE)
                .set(field(TransactionCreateRequest::amount), new BigDecimal("1001.00"))
                .create();

        // Act & Assert
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transaction refused. Please reduce the expense or add income to proceed."));
    }

    @Test
    void shouldPreventOverdraft_WhenConcurrentRequestsAreSent() throws Exception {
        // Arrange: Prepare the request payload for an EXPENSE of 900.00
        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(field(TransactionCreateRequest::categoryId), testCategoryExpense.getId())
                .set(field(TransactionCreateRequest::transactionType), TransactionType.EXPENSE)
                .set(field(TransactionCreateRequest::amount), new BigDecimal("900.00"))
                .create();

        String requestBody = objectMapper.writeValueAsString(request);

        // Setup Concurrency Utilities
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1); // The starting gun
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); // The finish line

        // Use AtomicInteger to safely count results across multiple threads
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: Create 2 parallel threads
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // All threads will wait here until the starting gun fires
                    startLatch.await();

                    // Fire the request to the API
                    int statusCode = mockMvc.perform(post("/transactions")
                                    .header("Authorization", "Bearer " + validToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn().getResponse().getStatus();

                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 400) { // Or 409 depending on your ExceptionHandler mapping
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Mark this thread as completed
                    doneLatch.countDown();
                }
            });
        }

        // FIRE THE STARTING GUN! Both threads execute the MockMvc request simultaneously
        startLatch.countDown();

        // Wait up to 5 seconds for both threads to finish processing
        boolean isCompleted = doneLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert:
        // Initial balance: 0. Limit: -1000.
        // Request 1 (-900) -> Accepted.
        // Request 2 (-900) -> Rejected (would result in -1800).
        assertTrue(isCompleted, "Transaction requests take over 5 seconds. System might be Deadlock");
        assertEquals(1, successCount.get(), "Only 1 transaction should be created successfully.");
        assertEquals(1, failCount.get(), "1 transaction must be rejected due to overdraft limits.");
    }
    //</editor-fold>

    //<editor-fold desc="GET /transactions/{transactionId}">
    @Test
    void shouldGetTransactionByIdSuccessfully() throws Exception {
        // Arrange: Save a transaction directly into DB
        Transaction transaction = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryExpense)
                .set(field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(field(Transaction::getAmount), new BigDecimal("250.50"))
                .set(field(Transaction::isActive), true)
                .create();
        transaction = transactionRepository.save(transaction);

        // Act & Assert
        mockMvc.perform(get("/transactions/{transactionId}", transaction.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId().toString()))
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.transactionType").value(TransactionType.EXPENSE.name()));
    }

    @Test
    void shouldReturnError_WhenTransactionNotFound() throws Exception {
        UUID fakeTransactionId = UUID.randomUUID();

        mockMvc.perform(get("/transactions/{transactionId}", fakeTransactionId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }
    //</editor-fold>

    //<editor-fold desc="GET /transactions (Pagination & Filtering)">
    @Test
    void shouldGetAllTransactionsWithPagination() throws Exception {
        // Arrange: Create multiple transactions
        Transaction t1 = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryExpense)
                .set(field(Transaction::isActive), true)
                .create();

        Transaction t2 = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryExpense)
                .set(field(Transaction::isActive), true)
                .create();

        transactionRepository.save(t1);
        transactionRepository.save(t2);

        // Act & Assert: Call endpoint expecting Slice wrapper format
        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + validToken)
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[*].id", hasItems(t1.getId().toString(), t2.getId().toString())));
    }
    //</editor-fold>

    //<editor-fold desc="PUT /transactions/{transactionId}">
    @Test
    void shouldUpdateTransactionSuccessfully() throws Exception {
        // Arrange: Create existing transaction
        Transaction transaction = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getTransactionType), testCategoryIncome.getTransactionType())
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryIncome)
                .set(field(Transaction::getAmount), new BigDecimal("100.00"))
                .set(field(Transaction::isActive), true)
                .create();
        transaction = transactionRepository.save(transaction);

        // New data for update
        TransactionUpdateRequest updateRequest = Instancio.of(TransactionUpdateRequest.class)
                .set(field(TransactionUpdateRequest::categoryId), testCategoryIncome.getId())
                .set(field(TransactionUpdateRequest::amount), new BigDecimal("999.99"))
                .set(field(TransactionUpdateRequest::note), "Updated note")
                .create();

        // Act & Assert
        mockMvc.perform(put("/transactions/{transactionId}", transaction.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.note").value("Updated note"));
    }
    //</editor-fold>

    //<editor-fold desc="DELETE /transactions/{transactionId}">
    @Test
    void shouldDeactivateTransactionSuccessfully() throws Exception {
        // Arrange
        Transaction transaction = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryExpense)
                .set(field(Transaction::isActive), true)
                .create();
        transaction = transactionRepository.save(transaction);

        // Act
        mockMvc.perform(delete("/transactions/{transactionId}", transaction.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        // Assert: Verify in DB
        Transaction updatedTransaction = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertFalse(updatedTransaction.isActive(), "The transaction should be deactivated");
    }
    //</editor-fold>

    //<editor-fold desc="GET /transactions/balance">
    @Test
    void shouldGetBalanceSuccessfully() throws Exception {
        // Arrange: Create INCOME and EXPENSE transactions
        Category incomeCategory = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getTransactionType), TransactionType.INCOME)
                .create();
        incomeCategory = categoryRepository.save(incomeCategory);

        Transaction incomeTx = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), incomeCategory)
                .set(field(Transaction::getTransactionType), TransactionType.INCOME)
                .set(field(Transaction::getAmount), new BigDecimal("1000.00"))
                .set(field(Transaction::isActive), true)
                .create();

        Transaction expenseTx = Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), testCategoryExpense) // testCategory is EXPENSE
                .set(field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(field(Transaction::getAmount), new BigDecimal("400.00"))
                .set(field(Transaction::isActive), true)
                .create();

        transactionRepository.save(incomeTx);
        transactionRepository.save(expenseTx);

        // Act & Assert
        // Expected balance = 1000.00 - 400.00 = 600.00
        mockMvc.perform(get("/transactions/balance")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpense").value(400.00))
                .andExpect(jsonPath("$.currentBalance").value(600.00));
    }
    //</editor-fold>

    //<editor-fold desc="POST /transactions (Budget Evaluation)">
    @ParameterizedTest(name = "Amount: {0}, Expected over budget flag: {1}, Expected warning: {2}")
    @MethodSource("provideBudgetTestCases")
    @Transactional
    void shouldHandleTransactionOverBudgetFlagAndBudgetWarnings(BigDecimal amount, boolean isOverBudget, String expectedWarningMessage) throws Exception {
        // Arrange
        Category expenseCategory = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .set(field(Category::isActive), true)
                .create();
        expenseCategory = categoryRepository.save(expenseCategory);

        Budget budget = Instancio.of(Budget.class)
                .ignore(field(Budget::getId))
                .set(field(Budget::getUser), testUser)
                .set(field(Budget::getCategory), expenseCategory)
                .set(field(Budget::getAmountLimit), BigDecimal.valueOf(100))
                .set(field(Budget::getBudgetMonth), LocalDate.now().getMonthValue())
                .set(field(Budget::getBudgetYear),  LocalDate.now().getYear())
                .set(field(Budget::isActive), true)
                .create();
        budgetRepository.save(budget);

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(field(TransactionCreateRequest::categoryId), expenseCategory.getId())
                .set(field(TransactionCreateRequest::amount), amount)
                .ignore(field(TransactionCreateRequest::transactionType))
                .create();

        // Act & Assert
        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isOverBudget").value(isOverBudget))
                .andExpect(jsonPath("$.warningMessage").value(expectedWarningMessage));
    }

    private static Stream<Arguments> provideBudgetTestCases() {
        return Stream.of(
                Arguments.of(BigDecimal.valueOf(50), false, null),
                Arguments.of(BigDecimal.valueOf(95), false, "You are approaching your budget limit for this category."),
                Arguments.of(BigDecimal.valueOf(102), true, "Transaction accepted, but you have exceeded your budget for this category.")
        );
    }
    //</editor-fold>
}
