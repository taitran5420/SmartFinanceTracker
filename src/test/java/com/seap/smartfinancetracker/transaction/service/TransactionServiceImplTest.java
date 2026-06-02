package com.seap.smartfinancetracker.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@SuppressWarnings("unused")
class TransactionServiceImplTest {
    //<editor-fold desc="Setup & Configurations">
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;
    //</editor-fold>

    //<editor-fold desc="Test createTransaction">
    @Test
    @DisplayName("Should throw exception when idempotency key already exists")
    void createTransaction_ShouldThrowException_WhenIdempotencyKeyExists() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("idempotencyKey"), idempotencyKey)
                .create();

        when(transactionRepository.existsByIdempotencyKey(idempotencyKey)).thenReturn(true);

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transactionService.createTransaction(userId, request)
        );

        assertEquals(HttpStatus.CONFLICT.value(), exception.getErrorCode().getHttpStatus());
        assertEquals("A transaction with this idempotency key already exists", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create a transaction using a specific user category")
    void createTransaction_ShouldSucceed_WhenUsingValidUserCategory() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        TransactionType type = TransactionType.EXPENSE;

        when(userRepository.findByIdWithPessimisticLock(userId))
                .thenReturn(Optional.of(Instancio.of(User.class)
                        .set(Select.field(User::getId), userId)
                        .create()));

        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                Mockito.any(UUID.class),
                Mockito.eq(TransactionType.INCOME)
        )).thenReturn(BigDecimal.valueOf(50000));

        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                Mockito.any(UUID.class),
                Mockito.eq(TransactionType.EXPENSE)
        )).thenReturn(BigDecimal.ZERO);

        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(any(UUID.class), any(UUID.class), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("categoryId"), categoryId)
                .set(Select.field("transactionType"), type)
                .create();

        User owner = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
        Category category = Instancio.of(Category.class)
                .set(Select.field(Category::getId), categoryId)
                .set(Select.field(Category::getUser), owner)
                .set(Select.field(Category::getTransactionType), type)
                .create();

        Transaction mappedEntity = Instancio.create(Transaction.class);
        Transaction savedEntity = Instancio.create(Transaction.class);
        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(transactionMapper.toEntity(userId, request, category, type)).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(transactionMapper.toResponse(savedEntity, null)).thenReturn(expectedResponse);
        when(categoryService.getCategoryEntity(eq(userId), eq(categoryId))).thenReturn(category);

        // 2. Act
        TransactionResponse actualResponse = transactionService.createTransaction(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(transactionRepository, times(1)).save(mappedEntity);
    }

    @Test
    @DisplayName("Should successfully create a transaction using a default system category when categoryId is null")
    void createTransaction_ShouldSucceed_WhenUsingDefaultSystemCategory() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        TransactionType type = TransactionType.INCOME;
        String systemDefaultCategoryCode = "SYS_OTHER_INCOME";

        when(userRepository.findByIdWithPessimisticLock(userId))
                .thenReturn(Optional.of(Instancio.of(User.class)
                        .set(Select.field(User::getId), userId)
                        .create()));

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("categoryId"), null)
                .set(Select.field("transactionType"), type)
                .create();

        Category defaultCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), null)
                .set(Select.field(Category::getTransactionType), type)
                .create();

        Transaction mappedEntity = Instancio.create(Transaction.class);
        Transaction savedEntity = Instancio.create(Transaction.class);
        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(categoryRepository.findByCode(eq(systemDefaultCategoryCode))).thenReturn(Optional.of(defaultCategory));
        when(transactionMapper.toEntity(userId, request, defaultCategory, type)).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(transactionMapper.toResponse(savedEntity, null)).thenReturn(expectedResponse);

        // 2. Act
        TransactionResponse actualResponse = transactionService.createTransaction(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(transactionRepository, times(1)).save(mappedEntity);
    }
    //</editor-fold>

    //<editor-fold desc="Test getTransactionById">
    @Test
    @DisplayName("Should successfully retrieve a transaction by ID")
    void getTransactionById_ShouldReturnResponse_WhenTransactionExists() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = Instancio.create(Transaction.class);
        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(expectedResponse);

        // 2. Act
        TransactionResponse actualResponse = transactionService.getTransactionById(userId, transactionId);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    @DisplayName("Should throw exception when retrieving a non-existent transaction")
    void getTransactionById_ShouldThrowException_WhenTransactionNotFound() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transactionService.getTransactionById(userId, transactionId)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getErrorCode().getHttpStatus());
        assertEquals("Transaction Not Found", exception.getMessage());
    }
    //</editor-fold>

    //<editor-fold desc="Test getTransactions (Filtering & Pagination)">
    @Test
    @DisplayName("Should return a slice of mapped transaction responses")
    @SuppressWarnings("unchecked")
    void getTransactions_ShouldReturnSliceOfResponses() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        TransactionFilterRequest filterRequest = Instancio.create(TransactionFilterRequest.class);
        Pageable pageable = PageRequest.of(0, 10);

        Transaction transaction1 = Instancio.create(Transaction.class);
        Transaction transaction2 = Instancio.create(Transaction.class);
        Page<Transaction> transactionSlice = new PageImpl<>(List.of(transaction1, transaction2));

        TransactionResponse response1 = Instancio.create(TransactionResponse.class);
        TransactionResponse response2 = Instancio.create(TransactionResponse.class);

        // Using ArgumentMatchers.any() for Specification because creating a specific instance is complex
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(transactionSlice);
        when(transactionMapper.toResponse(transaction1)).thenReturn(response1);
        when(transactionMapper.toResponse(transaction2)).thenReturn(response2);

        // 2. Act
        Slice<TransactionResponse> result = transactionService.getTransactions(userId, filterRequest, pageable);

        // 3. Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().containsAll(List.of(response1, response2)));
    }
    //</editor-fold>

    //<editor-fold desc="Test updateTransaction">
    @Test
    @DisplayName("Should successfully update transaction details (amount, note) and return response")
    void updateTransaction_ShouldUpdateAndReturnResponse_WhenValidRequest() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        BigDecimal newAmount = new BigDecimal("500.00");
        String newNote = "Updated Note";

        TransactionUpdateRequest request = Instancio.of(TransactionUpdateRequest.class)
                .set(Select.field("amount"), newAmount)
                .set(Select.field("note"), newNote)
                .set(Select.field("categoryId"), null) // No category change
                .create();

        Transaction existingTransaction = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getId), transactionId)
                .set(Select.field(Transaction::getTransactionType), TransactionType.INCOME)
                .create();

        Transaction savedTransaction = Instancio.create(Transaction.class);
        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(expectedResponse);

        // 2. Act
        TransactionResponse actualResponse = transactionService.updateTransaction(userId, transactionId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

        Transaction capturedTx = transactionArgumentCaptor.getValue();

        assertEquals(newAmount, capturedTx.getAmount(), "Amount should be updated in the new copy");
        assertEquals(newNote, capturedTx.getNote(), "Note should be updated in the new copy");
        assertEquals(existingTransaction.getId(), capturedTx.getId(), "Transaction ID must remain exactly the same");
    }
    //</editor-fold>

    //<editor-fold desc="Test deleteTransaction (Deactivate)">
    @Test
    @DisplayName("Should set transaction active status to false when it is currently active")
    void deleteTransaction_ShouldDeactivate_WhenTransactionIsActive() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction existingTransaction = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::isActive), true)
                .create();

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(existingTransaction));

        // 2. Act
        transactionService.deleteTransaction(userId, transactionId);

        // 3. Assert
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

        Transaction deactivatedTx = transactionArgumentCaptor.getValue();

        assertFalse(deactivatedTx.isActive(), "The new transaction copy should be deactivated");
        assertEquals(existingTransaction.getId(), deactivatedTx.getId(), "Transaction ID must remain exactly the same");
    }

    @Test
    @DisplayName("Should skip saving when transaction is already inactive")
    void deleteTransaction_ShouldDoNothing_WhenTransactionIsAlreadyInactive() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Transaction existingTransaction = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::isActive), false)
                .create();

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(existingTransaction));

        // 2. Act
        transactionService.deleteTransaction(userId, transactionId);

        // 3. Assert
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
    //</editor-fold>

    //<editor-fold desc="Test getBalanceByUserId">
    @Test
    @DisplayName("Should correctly calculate balance based on total income and total expense")
    void getBalanceByUserId_ShouldReturnCorrectBalanceResponse() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        BigDecimal totalIncome = new BigDecimal("1000.00");
        BigDecimal totalExpense = new BigDecimal("400.00");
        BigDecimal expectedBalance = new BigDecimal("600.00");

        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.INCOME))
                .thenReturn(totalIncome);
        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.EXPENSE))
                .thenReturn(totalExpense);

        // 2. Act
        BalanceResponse actualResponse = transactionService.getBalanceByUserId(userId);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(totalIncome, actualResponse.totalIncome());
        assertEquals(totalExpense, actualResponse.totalExpense());
        assertEquals(expectedBalance, actualResponse.currentBalance());
    }
    //</editor-fold>

    //<editor-fold desc="Test Budget Evaluation">
    @Test
    @DisplayName("Should quietly accept transaction when total usage is under 90 percent")
    void createTransaction_ShouldAcceptQuietly_WhenUsageIsUnder90Percent() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Budget is 1000, current spent is 800, new transaction is 50 => Projected total is 850 (85% - Under 90%)
        BigDecimal budgetLimit = new BigDecimal("1000.00");
        BigDecimal currentSpent = new BigDecimal("800.00");
        BigDecimal newAmount = new BigDecimal("50.00");

        setupMocksForBudgetEvaluation(userId, categoryId, budgetLimit, currentSpent);

        Transaction mappedEntity = Instancio.of(Transaction.class)
                .ignore(Select.field(Transaction::getId))
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::isOverBudget), false)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        Transaction savedEntity = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionMapper.toEntity(eq(userId), any(TransactionCreateRequest.class), any(), any())).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);

        // Expect warningMessage to be null since usage is under the 90% threshold
        when(transactionMapper.toResponse(eq(savedEntity), nullable(String.class))).thenReturn(expectedResponse);

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("categoryId"), categoryId)
                .set(Select.field("transactionType"), TransactionType.EXPENSE)
                .set(Select.field("amount"), newAmount)
                .create();

        // 2. Act
        TransactionResponse actualResponse = transactionService.createTransaction(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertFalse(mappedEntity.isOverBudget(), "Transaction should not be marked as over-budget");
        verify(transactionRepository, times(1)).save(mappedEntity);
    }

    @Test
    @DisplayName("Should return warning message when total usage is between 90 and 100 percent")
    void createTransaction_ShouldReturnWarning_WhenUsageIsBetween90And100Percent() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Budget is 1000, current spent is 800, new transaction is 150 => Projected total is 950 (95% - Between 90% and 100%)
        BigDecimal budgetLimit = new BigDecimal("1000.00");
        BigDecimal currentSpent = new BigDecimal("800.00");
        BigDecimal newAmount = new BigDecimal("150.00");

        setupMocksForBudgetEvaluation(userId, categoryId, budgetLimit, currentSpent);

        Transaction mappedEntity = Instancio.of(Transaction.class)
                .ignore(Select.field(Transaction::getId))
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::isOverBudget), false)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        Transaction savedEntity = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionMapper.toEntity(eq(userId), any(TransactionCreateRequest.class), any(), any())).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);

        when(transactionMapper.toResponse(eq(savedEntity), nullable(String.class))).thenReturn(expectedResponse);

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("categoryId"), categoryId)
                .set(Select.field("transactionType"), TransactionType.EXPENSE)
                .set(Select.field("amount"), newAmount)
                .create();

        // 2. Act
        TransactionResponse actualResponse = transactionService.createTransaction(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertFalse(mappedEntity.isOverBudget(), "Transaction should not be marked as over-budget yet");
    }

    @Test
    @DisplayName("Should mark transaction as over-budget and return warning when usage exceeds 100 percent")
    void createTransaction_ShouldMarkOverBudgetAndWarn_WhenUsageExceeds100Percent() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // Budget is 1000, current spent is 800, new transaction is 250 => Projected total is 1050 (105% - Exceeds 100%)
        BigDecimal budgetLimit = new BigDecimal("1000.00");
        BigDecimal currentSpent = new BigDecimal("800.00");
        BigDecimal newAmount = new BigDecimal("250.00");

        setupMocksForBudgetEvaluation(userId, categoryId, budgetLimit, currentSpent);

        Transaction mappedEntity = Instancio.of(Transaction.class)
                .ignore(Select.field(Transaction::getId))
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::isOverBudget), false)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        Transaction savedEntity = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), User.builder().id(userId).build())
                .set(Select.field(Transaction::getCategory), Category.builder().id(categoryId).build())
                .set(Select.field(Transaction::getAmount), newAmount)
                .set(Select.field(Transaction::isOverBudget), true)
                .set(Select.field(Transaction::getTransactionType),  TransactionType.EXPENSE)
                .create();

        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionMapper.toEntity(eq(userId), any(TransactionCreateRequest.class), any(), any())).thenReturn(mappedEntity);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedEntity);

        // Expect a warning message indicating the budget has been exceeded
        String expectedWarning = "Transaction accepted, but you have exceeded your budget for this category.";
        when(transactionMapper.toResponse(eq(savedEntity), eq(expectedWarning))).thenReturn(expectedResponse);

        TransactionCreateRequest request = Instancio.of(TransactionCreateRequest.class)
                .set(Select.field("categoryId"), categoryId)
                .set(Select.field("transactionType"), TransactionType.EXPENSE)
                .set(Select.field("amount"), newAmount)
                .create();

        // 2. Act
        TransactionResponse actualResponse = transactionService.createTransaction(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        // 4. Capture & Verify Immutable Data
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

        Transaction capturedTx = transactionArgumentCaptor.getValue();

        assertTrue(capturedTx.isOverBudget(), "Transaction MUST be marked as over-budget in the newly built copy");
        assertEquals(newAmount, capturedTx.getAmount(), "Amount must remain correct");
    }

    /**
     * Helper method to set up common mocks for budget evaluation tests.
     */
    private void setupMocksForBudgetEvaluation(UUID userId, UUID categoryId, BigDecimal budgetLimit, BigDecimal currentSpent) {
        User owner = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
        Category category = Instancio.of(Category.class)
                .set(Select.field(Category::getId), categoryId)
                .set(Select.field(Category::getUser), owner)
                .set(Select.field(Category::getTransactionType), TransactionType.EXPENSE)
                .create();

        Budget mockBudget = Instancio.of(Budget.class)
                .set(Select.field(Budget::isActive), true)
                .set(Select.field(Budget::getAmountLimit), budgetLimit)
                .create();

        // Mock passing initial validations
        when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);
        when(userRepository.findByIdWithPessimisticLock(userId)).thenReturn(Optional.of(owner));
        when(categoryService.getCategoryEntity(eq(userId), eq(categoryId))).thenReturn(category);

        // Bypass overdraft limit check from the existing createTransaction logic
        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(any(UUID.class), eq(TransactionType.INCOME)))
                .thenReturn(new BigDecimal("100000.00"));
        when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(any(UUID.class), eq(TransactionType.EXPENSE)))
                .thenReturn(BigDecimal.ZERO);

        // Mock DB Queries for Budget
        when(budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(
                eq(userId), eq(categoryId), anyInt(), anyInt()))
                .thenReturn(Optional.of(mockBudget));

        // Note: Adjust the repository method name if it differs in your actual implementation
        when(transactionRepository.calculateTotalSpentByCategoryAndMonth(
                eq(userId), eq(categoryId), anyInt(), anyInt()))
                .thenReturn(currentSpent);
    }
    //</editor-fold>
}
