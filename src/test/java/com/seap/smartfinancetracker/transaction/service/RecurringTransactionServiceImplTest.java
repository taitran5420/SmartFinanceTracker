package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionResponse;
import com.seap.smartfinancetracker.transaction.dto.RecurringTransactionUpdateRequest;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.exception.RecurringTransactionErrorCode;
import com.seap.smartfinancetracker.transaction.mapper.RecurringTransactionMapper;
import com.seap.smartfinancetracker.transaction.processor.RecurringTransactionProcessor;
import com.seap.smartfinancetracker.transaction.repository.RecurringTransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceImplTest {

    //<editor-fold desc="Setup & Mocks">
    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private RecurringTransactionMapper recurringTransactionMapper;

    @Mock
    private RecurringTransactionProcessor recurringTransactionProcessor;

    @InjectMocks
    private RecurringTransactionServiceImpl recurringTransactionService;

    @Captor
    private ArgumentCaptor<RecurringTransaction> recurringCaptor;
    //</editor-fold>

    //<editor-fold desc="Test createRecurring">
    @Test
    @DisplayName("Should successfully create a recurring transaction when category is valid and owned by user")
    void createRecurring_ShouldReturnResponse_WhenCategoryIsValid() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecurringTransactionCreateRequest request = Instancio.create(RecurringTransactionCreateRequest.class);
        Category mockCategory = Instancio.create(Category.class);
        RecurringTransaction mappedEntity = Instancio.create(RecurringTransaction.class);
        RecurringTransaction savedEntity = Instancio.create(RecurringTransaction.class);
        RecurringTransactionResponse expectedResponse = Instancio.create(RecurringTransactionResponse.class);

        when(categoryService.getCategoryEntity(userId, request.categoryId())).thenReturn(mockCategory);
        when(recurringTransactionMapper.toEntity(userId, request, mockCategory)).thenReturn(mappedEntity);
        when(recurringTransactionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(recurringTransactionMapper.toRecurringTransactionResponse(savedEntity)).thenReturn(expectedResponse);

        // Act
        RecurringTransactionResponse actualResponse = recurringTransactionService.createRecurring(userId, request);

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(recurringTransactionRepository, times(1)).save(mappedEntity);
    }
    //</editor-fold>

    //<editor-fold desc="Test updateRecurring">
    @Test
    @DisplayName("Should update recurring transaction successfully when transaction types match")
    void updateRecurring_ShouldUpdate_WhenTransactionTypesMatch() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        RecurringTransactionUpdateRequest request = Instancio.create(RecurringTransactionUpdateRequest.class);

        Category oldCategory = Instancio.of(Category.class)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .create();

        Category newCategory = Instancio.of(Category.class)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .create();

        RecurringTransaction existingRecurring = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getCategory), oldCategory)
                .create();

        RecurringTransactionResponse expectedResponse = Instancio.create(RecurringTransactionResponse.class);

        when(recurringTransactionRepository.findByIdAndUserId(recurringId, userId)).thenReturn(Optional.of(existingRecurring));

        when(categoryService.getCategoryEntity(userId, request.categoryId())).thenReturn(newCategory);

        when(recurringTransactionRepository.save(any(RecurringTransaction.class))).thenReturn(existingRecurring);
        when(recurringTransactionMapper.toRecurringTransactionResponse(any())).thenReturn(expectedResponse);

        // Act
        RecurringTransactionResponse actualResponse = recurringTransactionService.updateRecurring(userId, recurringId, request);

        // Assert
        assertNotNull(actualResponse);
        verify(recurringTransactionRepository).save(recurringCaptor.capture());
        RecurringTransaction savedEntity = recurringCaptor.getValue();

        // Verify that the new category was applied
        assertEquals(newCategory, savedEntity.getCategory(), "The category should be updated to the new one");
    }

    @Test
    @DisplayName("Should throw BusinessException when trying to change transaction type (e.g. INCOME to EXPENSE)")
    void updateRecurring_ShouldThrowException_WhenTransactionTypeConflicts() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        RecurringTransactionUpdateRequest request = Instancio.create(RecurringTransactionUpdateRequest.class);

        // Old category is EXPENSE
        Category oldCategory = Instancio.of(Category.class)
                .set(field(Category::getTransactionType), TransactionType.EXPENSE)
                .create();

        // New category is INCOME
        Category newCategory = Instancio.of(Category.class)
                .set(field(Category::getTransactionType), TransactionType.INCOME)
                .create();

        RecurringTransaction existingRecurring = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::getCategory), oldCategory)
                .create();

        when(recurringTransactionRepository.findByIdAndUserId(recurringId, userId)).thenReturn(Optional.of(existingRecurring));

        when(categoryService.getCategoryEntity(userId, request.categoryId())).thenReturn(newCategory);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> recurringTransactionService.updateRecurring(userId, recurringId, request)
        );

        assertEquals(RecurringTransactionErrorCode.UPDATE_CONFLICT_TRANSACTION_TYPE, exception.getErrorCode());
        verify(recurringTransactionRepository, never()).save(any());
    }
    //</editor-fold>

    //<editor-fold desc="Test toggleActiveStatus">
    @Test
    @DisplayName("Should toggle active status correctly from true to false")
    void toggleActiveStatus_ShouldInvertActiveFlag() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();

        RecurringTransaction existingRecurring = Instancio.of(RecurringTransaction.class)
                .set(field(RecurringTransaction::isActive), true)
                .create();

        when(recurringTransactionRepository.findByIdAndUserId(recurringId, userId)).thenReturn(Optional.of(existingRecurring));
        when(recurringTransactionRepository.save(any(RecurringTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recurringTransactionService.toggleActiveStatus(userId, recurringId);

        // Assert
        verify(recurringTransactionRepository).save(recurringCaptor.capture());
        RecurringTransaction savedEntity = recurringCaptor.getValue();

        assertFalse(savedEntity.isActive(), "The active status should be toggled to false");
    }
    //</editor-fold>

    //<editor-fold desc="Test processDueRecurringTransactions">
    @Test
    @DisplayName("Should fetch due transactions, group by user ID, and delegate to processor")
    void processDueRecurringTransactions_ShouldGroupAndDelegate() {
        // Arrange
        User user1 = Instancio.of(User.class).create();
        User user2 = Instancio.of(User.class).create();

        // 2 transactions for user1, 1 transaction for user2
        RecurringTransaction tx1User1 = Instancio.of(RecurringTransaction.class).set(field(RecurringTransaction::getUser), user1).create();
        RecurringTransaction tx2User1 = Instancio.of(RecurringTransaction.class).set(field(RecurringTransaction::getUser), user1).create();
        RecurringTransaction tx1User2 = Instancio.of(RecurringTransaction.class).set(field(RecurringTransaction::getUser), user2).create();

        List<RecurringTransaction> mockDueTransactions = List.of(tx1User1, tx2User1, tx1User2);

        when(recurringTransactionRepository.findDueTransactions(any(LocalDate.class), any(LocalTime.class)))
                .thenReturn(mockDueTransactions);

        // Act
        recurringTransactionService.processDueRecurringTransactions();

        // Assert
        // Verify user1's batch was processed (size 2)
        verify(recurringTransactionProcessor, times(1)).processRecurringTransactionForUser(
                eq(user1.getId()),
                argThat(list -> list.size() == 2 && list.contains(tx1User1) && list.contains(tx2User1))
        );

        // Verify user2's batch was processed (size 1)
        verify(recurringTransactionProcessor, times(1)).processRecurringTransactionForUser(
                eq(user2.getId()),
                argThat(list -> list.size() == 1 && list.contains(tx1User2))
        );
    }
    //</editor-fold>
}