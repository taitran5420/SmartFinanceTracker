package com.seap.smartfinancetracker.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.budget.entity.Budget;
import com.seap.smartfinancetracker.budget.repository.BudgetRepository;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.exception.CategoryErrorCode;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.notification.event.TransactionCreatedEvent;
import com.seap.smartfinancetracker.transaction.dto.*;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.exception.TransactionErrorCode;
import com.seap.smartfinancetracker.transaction.mapper.TransactionMapper;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.transaction.repository.TransactionSpecification;
import com.seap.smartfinancetracker.user.exception.UserErrorCode;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;

    private final TransactionMapper transactionMapper;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_EXPENSE_CODE = "SYS_OTHER_EXPENSE";
    private static final String DEFAULT_INCOME_CODE = "SYS_OTHER_INCOME";

    private static final String OVER_BUDGET_MESSAGE = "Transaction accepted, but you have exceeded your budget for this category.";
    private static final String WARNING_BUDGET_MESSAGE = "You are approaching your budget limit for this category.";

    private static final String MISSING_DEFAULT_CATEGORY_ERROR_MSG = "System Error: Missing '%s' category!";

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.idempotencyKey() != null && transactionRepository.existsByIdempotencyKey(transactionCreateRequest.idempotencyKey())) {
            log.warn("Duplicate transaction attempt detected with idempotency key: {}", transactionCreateRequest.idempotencyKey());
            throw new BusinessException(TransactionErrorCode.IDEMPOTENCY_KEY_EXISTS);
        }

        userRepository.findByIdWithPessimisticLock(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Category category = resolveCategory(userId, transactionCreateRequest);
        TransactionType finalTransactionType = category.getTransactionType();

        Transaction transaction = transactionMapper.toEntity(userId, transactionCreateRequest, category, finalTransactionType);

        if (finalTransactionType == TransactionType.EXPENSE) {
            validateOverdraftLimit(userId, transaction.getAmount());
        }

        String warningMessage = null;

        if (finalTransactionType == TransactionType.EXPENSE) {
            BudgetEvaluationResult evalResult = this.evaluateBudgetUsage(userId, category.getId(), transaction);
            if (evalResult != null) {
                warningMessage = evalResult.warningMessage();

                if (evalResult.isOverBudget()) {
                    transaction = transaction.toBuilder()
                            .isOverBudget(true).build();
                }
            }
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionCreatedEvent transactionCreatedEvent = new TransactionCreatedEvent(
                userId, category.getCategoryName(), savedTransaction.getAmount(), savedTransaction.getTransactionType()
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(transactionCreatedEvent);
            kafkaTemplate.send("transaction-created-topic", jsonPayload);
            log.info("Published TransactionCreatedEvent to Kafka topic 'transaction-created-topic' for user: {}", userId);

        } catch (JsonProcessingException e) {
            log.error("Failed to convert event to JSON for user: {}", userId, e);
        }
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

        Transaction.TransactionBuilder transactionBuilder = transaction.toBuilder();

        if (transactionUpdateRequest.categoryId() != null && !transactionUpdateRequest.categoryId().equals(transaction.getCategory().getId())) {
            Category newCategory = categoryService.getCategoryEntity(userId, transactionUpdateRequest.categoryId());
            if (newCategory == null) {
                throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
            }

            if (newCategory.getTransactionType() != transaction.getTransactionType()) {
                throw new BusinessException(TransactionErrorCode.TRANSACTION_TYPE_CANNOT_CHANGE);
            }

            transactionBuilder.category(newCategory);
        }

        if (transactionUpdateRequest.amount() != null) {

            BigDecimal newAmount = transactionUpdateRequest.amount();

            if (transaction.getTransactionType().equals(TransactionType.EXPENSE))
            {
                validateOverdraftLimit(userId, newAmount);
            }

            transactionBuilder.amount(newAmount);
        }

        if (transactionUpdateRequest.note() != null) {
            transactionBuilder.note(transactionUpdateRequest.note());
        }

        Transaction updatedTransaction = transactionBuilder.build();
        Transaction savedTransaction = transactionRepository.save(updatedTransaction);

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

        Transaction deactivateTransaction = transaction.toBuilder()
                .active(false)
                .build();
        transactionRepository.save(deactivateTransaction);
        log.info("Transaction {} is deactivated", transactionId);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByUserId(UUID userId) {
        return getBalanceResponseByUserId(userId);
    }

    private Category resolveCategory(UUID userId, TransactionCreateRequest transactionCreateRequest) {
        if (transactionCreateRequest.categoryId() != null) {
            Category category = categoryService.getCategoryEntity(userId, transactionCreateRequest.categoryId());

            if (transactionCreateRequest.transactionType() != null && transactionCreateRequest.transactionType() != category.getTransactionType()) {
                log.error("Conflict: Request type {} does not match Category type {}",
                        transactionCreateRequest.transactionType(), category.getTransactionType());
                throw new BusinessException(TransactionErrorCode.TRANSACTION_TYPE_CONFLICT);
            }

            return category;
        }

        String systemDefaultCategoryCode = (transactionCreateRequest.transactionType() == TransactionType.EXPENSE) ? DEFAULT_EXPENSE_CODE :
                DEFAULT_INCOME_CODE;

        return categoryRepository.findByCode(systemDefaultCategoryCode)
                .orElseThrow(() -> {
                    log.error("System configuration error: Missing default category for code {}", systemDefaultCategoryCode);
                    return new IllegalStateException(String.format(MISSING_DEFAULT_CATEGORY_ERROR_MSG, systemDefaultCategoryCode));
                });
    }

    private Transaction getTransactionByUserIdAndTransactionId(UUID userId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new BusinessException(TransactionErrorCode.TRANSACTION_NOT_FOUND));
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
            throw new BusinessException(TransactionErrorCode.OVERDRAFT_LIMIT_EXCEEDED);
        }
    }

    private BudgetEvaluationResult evaluateBudgetUsage(UUID userId, UUID categoryId, Transaction newTransaction) {
        Instant transactionTime = newTransaction.getCreatedAt() != null ? newTransaction.getCreatedAt() : Instant.now();
        ZonedDateTime zdt = transactionTime.atZone(ZoneId.systemDefault());
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
            return new BudgetEvaluationResult(true, OVER_BUDGET_MESSAGE);
        } else if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return new BudgetEvaluationResult(false, WARNING_BUDGET_MESSAGE);
        }

        return null;
    }

    private record BudgetEvaluationResult(boolean isOverBudget, String warningMessage) {}
}
