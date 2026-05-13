package com.seap.smartfinancetracker.transaction.service;

import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.transaction.repository.TransactionSpecification;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of the {@link TransactionService}.
 * <p>
 * This class orchestrates the core business logic for transactions. It includes
 * critical financial safeguards such as idempotency validation, pessimistic locking
 * to prevent race conditions during concurrent balance updates, strict category
 * ownership checks, and overdraft limit enforcements.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;

    private final TransactionMapper transactionMapper;

    private static final String DEFAULT_EXPENSE_CODE = "SYS_OTHER_EXPENSE";
    private static final String DEFAULT_INCOME_CODE = "SYS_OTHER_INCOME";

    /**
     * The maximum allowed negative balance.
     * Users cannot process an expense that drops their balance below this limit.
     */
    private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("-1000");

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> This method utilizes a pessimistic write lock
     * on the User record to prevent concurrent transaction modifications. It resolves
     * the {@link TransactionType} either from a provided category or assigns a system
     * default category if only the type is provided. Then, it validates against
     * the {@code OVERDRAFT_LIMIT} before persisting an expense. Finally, it validates with category's budget
     * </p>
     */
    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.idempotencyKey() != null && transactionRepository.existsByIdempotencyKey(transactionCreateRequest.idempotencyKey())) {
            log.warn("Duplicate transaction attempt detected with idempotency key: {}", transactionCreateRequest.idempotencyKey());
            throw new IllegalArgumentException("A transaction with this idempotency key already exists!");
        }

        userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Category category;
        TransactionType finalTransactionType;

        if (transactionCreateRequest.categoryId() != null) {
            category = categoryRepository.findById(transactionCreateRequest.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

            if (isNotCategoryOwner(userId, category)) {
                log.warn("User {} attempted to use unauthorized category {}", userId, category.getId());
                throw new IllegalArgumentException("You do not have permission to use this category!");
            }

            if (transactionCreateRequest.transactionType() != null && transactionCreateRequest.transactionType() != category.getTransactionType()) {
                log.error("Conflict: Request type {} does not match Category type {}",
                        transactionCreateRequest.transactionType(), category.getTransactionType());
                throw new IllegalArgumentException("Transaction type does not match the category's defined type!");
            }

            finalTransactionType = category.getTransactionType();

        } else {
            finalTransactionType = transactionCreateRequest.transactionType();
            String systemDefaultCategoryCode = finalTransactionType == TransactionType.EXPENSE ? DEFAULT_EXPENSE_CODE :
                    DEFAULT_INCOME_CODE;

            category = categoryRepository.findByCode(systemDefaultCategoryCode)
                    .orElseThrow(() -> {
                        log.error("System configuration error: Missing default category for code {}", systemDefaultCategoryCode);
                        return new IllegalStateException("System Error: Missing " + systemDefaultCategoryCode + " category!");
                    });
        }

        Transaction transaction = transactionMapper.toEntity(userId, transactionCreateRequest);
        transaction.setCategory(category);
        transaction.setTransactionType(finalTransactionType);

        if (finalTransactionType == TransactionType.EXPENSE) {
            validateOverdraftLimit(userId, transaction.getAmount());
        }

        String warningMessage = null;

        if (finalTransactionType == TransactionType.EXPENSE) {
            warningMessage = this.evaluateBudgetUsage(userId, category.getId(), transaction);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(savedTransaction, warningMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID userId, UUID transactionId) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        return transactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<TransactionResponse> getTransactions(UUID userId, TransactionFilterRequest transactionFilterRequest, Pageable pageable) {
        Specification<Transaction> specification = TransactionSpecification.getFilterTransaction(userId, transactionFilterRequest);

        Slice<Transaction> transactionSlice = transactionRepository
                .findAll(specification, pageable);

        return transactionSlice.map(transactionMapper::toResponse);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Ensures that if a new category is provided,
     * it belongs to the user and its transaction type strictly matches the original
     * transaction's type.
     * </p>
     */
    @Override
    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionUpdateRequest transactionUpdateRequest) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        if (transactionUpdateRequest.categoryId() != null && !transactionUpdateRequest.categoryId().equals(transaction.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(transactionUpdateRequest.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

            if (isNotCategoryOwner(userId, newCategory)) {
                throw new IllegalArgumentException("You do not have permission to use this category!");
            }

            if (newCategory.getTransactionType() != transaction.getTransactionType()) {
                throw new IllegalArgumentException("Cannot change category to a different transaction type!");
            }

            transaction.setCategory(newCategory);
        }

        if (transactionUpdateRequest.amount() != null) {
            transaction.setAmount(transactionUpdateRequest.amount());
        }

        if (transactionUpdateRequest.note() != null) {
            transaction.setNote(transactionUpdateRequest.note());
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Safely ignores the request if the transaction
     * is already deactivated.
     * </p>
     */
    @Override
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = getTransactionByUserIdAndTransactionId(userId, transactionId);

        if (!transaction.isActive()) {
            log.info("Transaction {} is already deactivated", transactionId);
            return;
        }

        transaction.setActive(false);
        transactionRepository.save(transaction);
        log.info("Transaction {} is deactivated", transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByUserId(UUID userId) {
        return getBalanceResponseByUserId(userId);
    }

    private boolean isNotCategoryOwner(UUID userId, Category category) {
        if (category.getUser() == null) {
            return false;
        }

        return !category.getUser().getId().equals(userId);
    }

    private Transaction getTransactionByUserIdAndTransactionId(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found!"));
    }

    private BalanceResponse getBalanceResponseByUserId(UUID userId) {
        BigDecimal totalIncome = transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.INCOME);
        BigDecimal totalExpense =  transactionRepository.calculateTotalAmountByUserIdAndTransactionType(userId, TransactionType.EXPENSE);

        BigDecimal currentBalance = totalIncome.subtract(totalExpense);

        return BalanceResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .currentBalance(currentBalance)
                .build();
    }

    private void validateOverdraftLimit(UUID userId, BigDecimal expenseAmount) {
        BigDecimal currentBalance = getBalanceResponseByUserId(userId).currentBalance();

        BigDecimal hypotheticalBalance = currentBalance.subtract(expenseAmount);

        if (hypotheticalBalance.compareTo(OVERDRAFT_LIMIT) < 0) {
            log.warn("Overdraft prevented for user {}. Hypothetical balance: {}", userId, hypotheticalBalance);
            throw new IllegalArgumentException("Transaction refused. Please reduce the expense or add income to proceed.");
        }
    }

    private String evaluateBudgetUsage(UUID userId, UUID categoryId, Transaction newTransaction) {
        Instant now = Instant.now();
        ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
        int month = zdt.getMonthValue();
        int year = zdt.getYear();

        Optional<Budget> optionalBudget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonthAndBudgetYear(userId,
                categoryId, month, year);

        if (optionalBudget.isEmpty()) {
            return null;
        }

        Budget budget = optionalBudget.get();

        BigDecimal budgetLimit = budget.getAmountLimit();

        if (!budget.isActive() || budgetLimit.equals(BigDecimal.ZERO)) {
            return null;
        }

        BigDecimal totalSpent = transactionRepository.calculateTotalSpentByCategoryAndMonth(userId, categoryId,
                month, year).add(newTransaction.getAmount());

        BigDecimal percentage = totalSpent.divide(budgetLimit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            newTransaction.setOverBudget(true);
            return "Transaction accepted, but you have exceeded your budget for this category.";
        } else if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "You are approaching your budget limit for this category.";
        }

        return null;
    }
}
