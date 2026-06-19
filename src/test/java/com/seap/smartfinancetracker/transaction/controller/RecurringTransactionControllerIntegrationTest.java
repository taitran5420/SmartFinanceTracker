package com.seap.smartfinancetracker.transaction.controller;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RecurringTransactionControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private UserPrincipalMapper userPrincipalMapper;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;
    private Category testExpenseCategory;

    @BeforeEach
    void setUp() {
        recurringTransactionRepository.deleteAll();
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
        UserPrincipal userPrincipal = userPrincipalMapper.toUserPrincipal(testUser);
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }
    //</editor-fold>

    //<editor-fold desc="POST /recurring-transactions">
    @Test
    @DisplayName("Should successfully create a recurring transaction")
    void shouldCreateRecurringTransactionSuccessfully() throws Exception {
        // Arrange
        RecurringTransactionCreateRequest request = RecurringTransactionCreateRequest.builder()
                .categoryId(testExpenseCategory.getId())
                .amount(new BigDecimal("150.50"))
                .note("Monthly Netflix Subscription")
                .frequency(Frequency.MONTHLY)
                .startDate(LocalDate.now())
                .executionTime(LocalTime.of(8, 0))
                .build();

        // Act & Assert
        mockMvc.perform(post("/recurring-transactions")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.frequency").value(Frequency.MONTHLY.name()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.categoryId").value(testExpenseCategory.getId().toString()));
    }
    //</editor-fold>

    //<editor-fold desc="GET /recurring-transactions/{id}">
    @Test
    @DisplayName("Should get recurring transaction by ID successfully")
    void shouldGetRecurringTransactionById() throws Exception {
        // Arrange
        RecurringTransaction recurring = Instancio.of(RecurringTransaction.class)
                .ignore(field(RecurringTransaction::getId))
                .set(field(RecurringTransaction::getUser), testUser)
                .set(field(RecurringTransaction::getCategory), testExpenseCategory)
                .set(field(RecurringTransaction::getAmount), new BigDecimal("500.00"))
                .set(field(RecurringTransaction::getTransactionType), TransactionType.EXPENSE)
                .create();
        recurring = recurringTransactionRepository.save(recurring);

        // Act & Assert
        mockMvc.perform(get("/recurring-transactions/{id}", recurring.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recurring.getId().toString()))
                .andExpect(jsonPath("$.amount").value(500.00));
    }
    //</editor-fold>

    //<editor-fold desc="PUT /recurring-transactions/{id}">
    @Test
    @DisplayName("Should update recurring transaction successfully")
    void shouldUpdateRecurringTransaction() throws Exception {
        // Arrange
        RecurringTransaction recurring = Instancio.of(RecurringTransaction.class)
                .ignore(field(RecurringTransaction::getId))
                .set(field(RecurringTransaction::getUser), testUser)
                .set(field(RecurringTransaction::getCategory), testExpenseCategory)
                .set(field(RecurringTransaction::getAmount), new BigDecimal("200.00"))
                .set(field(RecurringTransaction::getTransactionType), TransactionType.EXPENSE)
                .create();
        recurring = recurringTransactionRepository.save(recurring);

        RecurringTransactionUpdateRequest updateRequest = RecurringTransactionUpdateRequest.builder()
                .amount(new BigDecimal("999.99"))
                .note("Updated Note")
                .build();

        // Act & Assert
        mockMvc.perform(put("/recurring-transactions/{id}", recurring.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.note").value("Updated Note"));
    }
    //</editor-fold>

    //<editor-fold desc="DELETE /recurring-transactions/{id}">
    @Test
    @DisplayName("Should delete recurring transaction successfully")
    void shouldDeleteRecurringTransaction() throws Exception {
        // Arrange
        RecurringTransaction recurring = Instancio.of(RecurringTransaction.class)
                .ignore(field(RecurringTransaction::getId))
                .set(field(RecurringTransaction::getUser), testUser)
                .set(field(RecurringTransaction::getCategory), testExpenseCategory)
                .create();
        recurring = recurringTransactionRepository.save(recurring);

        // Act & Assert
        mockMvc.perform(delete("/recurring-transactions/{id}", recurring.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        // Verify it was actually deleted from DB
        Optional<RecurringTransaction> deleted = recurringTransactionRepository.findById(recurring.getId());
        assertTrue(deleted.isEmpty(), "Recurring transaction should be hard deleted from database");
    }
    //</editor-fold>

    //<editor-fold desc="PATCH /recurring-transactions/{id}/toggle">
    @Test
    @DisplayName("Should toggle active status from true to false")
    void shouldToggleActiveStatus() throws Exception {
        // Arrange
        RecurringTransaction recurring = Instancio.of(RecurringTransaction.class)
                .ignore(field(RecurringTransaction::getId))
                .set(field(RecurringTransaction::getUser), testUser)
                .set(field(RecurringTransaction::getCategory), testExpenseCategory)
                .set(field(RecurringTransaction::isActive), true)
                .set(field(RecurringTransaction::getTransactionType), TransactionType.EXPENSE)
                .create();
        recurring = recurringTransactionRepository.save(recurring);

        // Act & Assert
        mockMvc.perform(patch("/recurring-transactions/{id}/toggle", recurring.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Verify in DB
        RecurringTransaction toggled = recurringTransactionRepository.findById(recurring.getId()).orElseThrow();
        assertFalse(toggled.isActive(), "The active flag should be false in database");
    }
    //</editor-fold>

    //<editor-fold desc="GET /recurring-transactions/upcoming">
    @Test
    @DisplayName("Should return upcoming transactions within the next 7 days")
    void shouldReturnUpcomingTransactions() throws Exception {
        // Arrange: Create a transaction that occurs tomorrow
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        RecurringTransaction upcomingRecurring = Instancio.of(RecurringTransaction.class)
                .ignore(field(RecurringTransaction::getId))
                .set(field(RecurringTransaction::getUser), testUser)
                .set(field(RecurringTransaction::getCategory), testExpenseCategory)
                .set(field(RecurringTransaction::getNextOccurrenceDate), tomorrow)
                .set(field(RecurringTransaction::isActive), true)
                .set(field(RecurringTransaction::getTransactionType), TransactionType.EXPENSE)
                .create();
        recurringTransactionRepository.save(upcomingRecurring);

        // Act & Assert
        mockMvc.perform(get("/recurring-transactions/upcoming")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].daysUntilDue").value(1));
    }
    //</editor-fold>
}