package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TransactionServiceImplTest {
    //<editor-fold desc="Setup & Configurations">
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionMapper transactionMapper;

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
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(userId, request)
        );

        assertEquals("A transaction with this idempotency key already exists!", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create a transaction using a specific user category")
    void createTransaction_ShouldSucceed_WhenUsingValidUserCategory() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        TransactionType type = TransactionType.EXPENSE;

        Mockito.when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                Mockito.any(UUID.class),
                Mockito.eq(TransactionType.INCOME)
        )).thenReturn(BigDecimal.valueOf(50000));

        Mockito.when(transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                Mockito.any(UUID.class),
                Mockito.eq(TransactionType.EXPENSE)
        )).thenReturn(BigDecimal.ZERO);

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
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(userId, request)).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(transactionMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

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
        when(categoryRepository.findFirstByUserIdIsNullAndTransactionType(type)).thenReturn(Optional.of(defaultCategory));
        when(transactionMapper.toEntity(userId, request)).thenReturn(mappedEntity);
        when(transactionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(transactionMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

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
        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.getTransactionById(userId, transactionId)
        );
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

        Transaction existingTransaction = Instancio.create(Transaction.class);
        TransactionResponse expectedResponse = Instancio.create(TransactionResponse.class);

        when(transactionRepository.findByIdAndUserId(transactionId, userId)).thenReturn(Optional.of(existingTransaction));
        when(transactionRepository.save(existingTransaction)).thenReturn(existingTransaction);
        when(transactionMapper.toResponse(existingTransaction)).thenReturn(expectedResponse);

        // 2. Act
        TransactionResponse actualResponse = transactionService.updateTransaction(userId, transactionId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        assertEquals(newAmount, existingTransaction.getAmount());
        assertEquals(newNote, existingTransaction.getNote());
        verify(transactionRepository, times(1)).save(existingTransaction);
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
        assertFalse(existingTransaction.isActive(), "Transaction should be deactivated");
        verify(transactionRepository, times(1)).save(existingTransaction);
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
}
