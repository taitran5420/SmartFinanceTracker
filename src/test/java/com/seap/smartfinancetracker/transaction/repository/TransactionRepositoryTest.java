package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.user.entity.User;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;
    //</editor-fold>

    //<editor-fold desc="Test findByIdAndUserId">
    @Test
    @DisplayName("Should return the transaction when both Transaction ID and User ID match securely")
    void findByIdAndUserId_ShouldReturnTransaction_WhenOwnedByGivenUser() {
        // Arrange: Create owner and category (required due to FK constraints)
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        // Arrange: Create transaction for owner
        Transaction transaction = transactionRepository.save(
                Instancio.of(Transaction.class)
                        .set(Select.field(Transaction::getUser), owner)
                        .set(Select.field(Transaction::getCategory), category)
                        .ignore(Select.field(Transaction::getId))
                        .create()
        );

        // Act
        Optional<Transaction> result = transactionRepository.findByIdAndUserId(transaction.getId(), owner.getId());

        // Assert
        assertTrue(result.isPresent(), "Transaction should be found when requested by its rightful owner");
        assertEquals(transaction.getId(), result.get().getId(), "The IDs must match exactly");
    }

    @Test
    @DisplayName("Should return empty Optional when a user tries to access another user's transaction (IDOR prevention)")
    void findByIdAndUserId_ShouldReturnEmpty_WhenNotOwnedByGivenUser() {
        // Arrange: Create owner, attacker, and category
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        User attacker = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        // Arrange: Create transaction belonging to the true owner
        Transaction transaction = transactionRepository.save(
                Instancio.of(Transaction.class)
                        .set(Select.field(Transaction::getUser), owner)
                        .set(Select.field(Transaction::getCategory), category)
                        .ignore(Select.field(Transaction::getId))
                        .create()
        );

        // Act: Attacker attempts to fetch the transaction
        Optional<Transaction> result = transactionRepository.findByIdAndUserId(transaction.getId(), attacker.getId());

        // Assert
        assertTrue(result.isEmpty(), "Transaction should NOT be found when requested by a different user");
    }
    //</editor-fold>

    //<editor-fold desc="Test existsByIdempotencyKey">
    @Test
    @DisplayName("Should return true when checking existence of an existing idempotency key")
    void existsByIdempotencyKey_ShouldReturnTrue_WhenKeyExists() {
        // Arrange
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(Instancio.of(Category.class).set(Select.field(Category::getUser), owner).ignore(Select.field(Category::getId)).create());

        UUID idempotencyKey = UUID.randomUUID();
        transactionRepository.save(
                Instancio.of(Transaction.class)
                        .set(Select.field(Transaction::getUser), owner)
                        .set(Select.field(Transaction::getCategory), category)
                        .set(Select.field(Transaction::getIdempotencyKey), idempotencyKey)
                        .ignore(Select.field(Transaction::getId))
                        .create()
        );

        // Act
        boolean exists = transactionRepository.existsByIdempotencyKey(idempotencyKey);

        // Assert
        assertTrue(exists, "Should return true for an existing idempotency key");
    }

    @Test
    @DisplayName("Should return false when checking existence of a non-existent idempotency key")
    void existsByIdempotencyKey_ShouldReturnFalse_WhenKeyDoesNotExist() {
        // Act
        boolean exists = transactionRepository.existsByIdempotencyKey(UUID.randomUUID());

        // Assert
        assertFalse(exists, "Should return false for a randomly generated, non-existent idempotency key");
    }
    //</editor-fold>

    //<editor-fold desc="Test calculateTotalAmountByUserIdAndTransactionType">
    @Test
    @DisplayName("Should calculate correct total amount for a specific user, specific transaction type, strictly matching active=true")
    void calculateTotalAmountByUserIdAndTransactionType_ShouldCalculateCorrectly() {
        // Arrange: Setup user and category
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        // T1: Valid INCOME, Active -> Include (100.0000)
        Transaction t1 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.INCOME)
                .set(Select.field(Transaction::getAmount), new BigDecimal("100.0000"))
                .set(Select.field(Transaction::isActive), true)
                .ignore(Select.field(Transaction::getId))
                .create();

        // T2: Valid INCOME, Active -> Include (50.0000)
        Transaction t2 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.INCOME)
                .set(Select.field(Transaction::getAmount), new BigDecimal("50.0000"))
                .set(Select.field(Transaction::isActive), true)
                .ignore(Select.field(Transaction::getId))
                .create();

        // T3: INCOME but Inactive -> Exclude (200.0000)
        Transaction t3 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.INCOME)
                .set(Select.field(Transaction::getAmount), new BigDecimal("200.0000"))
                .set(Select.field(Transaction::isActive), false)
                .ignore(Select.field(Transaction::getId))
                .create();

        // T4: EXPENSE, Active -> Exclude (30.0000)
        Transaction t4 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(Select.field(Transaction::getAmount), new BigDecimal("30.0000"))
                .set(Select.field(Transaction::isActive), true)
                .ignore(Select.field(Transaction::getId))
                .create();

        transactionRepository.saveAll(List.of(t1, t2, t3, t4));

        // Act
        BigDecimal totalIncome = transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                owner.getId(), TransactionType.INCOME
        );

        // Assert: 100.0000 + 50.0000 = 150.0000
        assertNotNull(totalIncome, "Total should not be null");
        assertEquals(0, new BigDecimal("150.0000").compareTo(totalIncome), "The calculated total amount must strictly match the sum of active transactions for the specified type");
    }

    @Test
    @DisplayName("Should return null (or zero based on COALESCE) when no transactions match the criteria")
    void calculateTotalAmountByUserIdAndTransactionType_ShouldReturnNullOrZero_WhenNoMatch() {
        // Arrange
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());

        // Act
        BigDecimal result = transactionRepository.calculateTotalAmountByUserIdAndTransactionType(
                owner.getId(), TransactionType.EXPENSE
        );

        // Assert: COALESCE(SUM(t.amount), 0) without a fallback parameter usually returns null in standard JPQL if no rows match, but returning null here is expected if the sum is completely empty.
        assertEquals(BigDecimal.valueOf(0.0), result,"Should return 0 if there are no transactions matching the user and type");
    }
    //</editor-fold>

    //<editor-fold desc="Test calculateTotalSpentByCategoryAndMonth">
    @Test
    @DisplayName("Should calculate correct spent amount for a specific user, category, month, year")
    void calculateTotalSpentByCategoryAndMonth_ShouldCalculateCorrectly() {
        // Arrange: Setup user and category
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .set(Select.field(Category::isActive), true)
                        .set(Select.field(Category::getTransactionType),  TransactionType.EXPENSE)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        // T1: Valid EXPENSE, Active -> Include (100.0000)
        Transaction t1 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(Select.field(Transaction::getAmount), new BigDecimal("100.0000"))
                .set(Select.field(Transaction::isActive), true)
                .ignore(Select.field(Transaction::getCreatedAt))
                .ignore(Select.field(Transaction::getId))
                .create();

        // T2: Valid EXPENSE, Active -> Include (50.0000)
        Transaction t2 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(Select.field(Transaction::getAmount), new BigDecimal("50.0000"))
                .set(Select.field(Transaction::isActive), true)
                .ignore(Select.field(Transaction::getCreatedAt))
                .ignore(Select.field(Transaction::getId))
                .create();

        // T3: EXPENSE but Inactive -> Exclude (200.0000)
        Transaction t3 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(Select.field(Transaction::getAmount), new BigDecimal("200.0000"))
                .set(Select.field(Transaction::isActive), false)
                .ignore(Select.field(Transaction::getCreatedAt))
                .ignore(Select.field(Transaction::getId))
                .create();

        // T4: EXPENSE, Active, last month -> Exclude (30.0000)
        Instant now = Instant.now();
        ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
        ZonedDateTime lastMonthZdt = zdt.minusMonths(1);
        Instant lastMonthZdtInstant = lastMonthZdt.toInstant();

        Transaction t4 = Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(Select.field(Transaction::getAmount), new BigDecimal("30.0000"))
                .set(Select.field(Transaction::isActive), true)
                .set(Select.field(Transaction::getCreatedAt), lastMonthZdtInstant)
                .ignore(Select.field(Transaction::getId))
                .create();

        transactionRepository.saveAll(List.of(t1, t2, t3, t4));

        // Act
        BigDecimal totalSpent = transactionRepository.calculateTotalSpentByCategoryAndMonth(
                owner.getId(), category.getId(), LocalDate.now().getMonthValue(), LocalDate.now().getYear()
        );

        // Assert: 100.0000 + 50.0000 = 150.0000
        assertNotNull(totalSpent, "Total spent should not be null");
        assertEquals(0, new BigDecimal("150.0000").compareTo(totalSpent), "The calculated total spent must strictly match the sum of active transactions for the specified category, month and year");
    }

    @Test
    @DisplayName("Should return zero based on COALESCE when no transactions spent match the criteria")
    void calculateTotalSpentByCategoryAndMonth_ShouldReturnNullOrZero_WhenNoMatch() {
        // Arrange
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .set(Select.field(Category::isActive), true)
                        .set(Select.field(Category::getTransactionType),  TransactionType.EXPENSE)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        // Act
        BigDecimal totalSpent = transactionRepository.calculateTotalSpentByCategoryAndMonth(
                owner.getId(), category.getId(), LocalDate.now().getMonthValue(), LocalDate.now().getYear()
        );

        // Assert: COALESCE(SUM(t.amount), 0) without a fallback parameter usually returns null in standard JPQL if no rows match, but returning null here is expected if the sum is completely empty.
        assertEquals(BigDecimal.valueOf(0.0), totalSpent,"Should return 0 if there are no transactions matching the criteria");
    }
    //</editor-fold>
}
