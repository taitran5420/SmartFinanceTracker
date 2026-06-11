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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
                .create();

        transactionRepository.saveAll(List.of(t1, t2, t3));

        entityManager.getEntityManager().createNativeQuery(
                        "INSERT INTO transactions (id, user_id, category_id, amount, transaction_type, active, created_at, updated_at) " +
                                "VALUES (:id, :userId, :categoryId, :amount, CAST(:type AS transaction_type_enum), :active, :pastTime, :pastTime)"
                )
                .setParameter("id", t4.getId())
                .setParameter("userId", t4.getUser().getId())
                .setParameter("categoryId", t4.getCategory().getId())
                .setParameter("amount", t4.getAmount())
                .setParameter("type",t4.getTransactionType().name())
                .setParameter("active", true)
                .setParameter("pastTime", lastMonthZdtInstant)
                .executeUpdate();
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

    //<editor-fold desc="Analytics query helpers">
    /**
     * Persists a user with a generated id.
     */
    private User newUser() {
        return entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
    }

    /**
     * Persists an active category of the given name and type for the owner.
     */
    private Category newCategory(User owner, String name, TransactionType type) {
        return entityManager.persistAndFlush(Instancio.of(Category.class)
                .set(Select.field(Category::getUser), owner)
                .set(Select.field(Category::getCategoryName), name)
                .set(Select.field(Category::getTransactionType), type)
                .set(Select.field(Category::isActive), true)
                .ignore(Select.field(Category::getId))
                .create());
    }

    /**
     * Saves a transaction whose {@code createdAt} defaults to "now" (the field initializer is
     * left untouched by ignoring it), so it falls inside the current calendar month.
     */
    private void saveNow(User owner, Category category, TransactionType type, String amount, boolean active) {
        transactionRepository.save(Instancio.of(Transaction.class)
                .set(Select.field(Transaction::getUser), owner)
                .set(Select.field(Transaction::getCategory), category)
                .set(Select.field(Transaction::getTransactionType), type)
                .set(Select.field(Transaction::getAmount), new BigDecimal(amount))
                .set(Select.field(Transaction::isActive), active)
                .ignore(Select.field(Transaction::getCreatedAt))
                .ignore(Select.field(Transaction::getId))
                .create());
    }

    /**
     * Inserts a transaction with an explicit {@code createdAt} via native SQL. JPA auditing /
     * the field initializer would otherwise stamp the row with "now", so back-dating requires
     * bypassing the entity layer (the same technique used by the month/year test above).
     */
    private void insertAt(User owner, Category category, TransactionType type, String amount, boolean active, Instant when) {
        entityManager.getEntityManager().createNativeQuery(
                        "INSERT INTO transactions (id, user_id, category_id, amount, transaction_type, active, created_at, updated_at) " +
                                "VALUES (:id, :userId, :categoryId, :amount, CAST(:type AS transaction_type_enum), :active, :when, :when)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("userId", owner.getId())
                .setParameter("categoryId", category.getId())
                .setParameter("amount", new BigDecimal(amount))
                .setParameter("type", type.name())
                .setParameter("active", active)
                .setParameter("when", when)
                .executeUpdate();
    }
    //</editor-fold>

    //<editor-fold desc="Test findCategorySpending">
    @Test
    @DisplayName("Should group expenses by category ordered by descending spend, excluding income, inactive and other users")
    void findCategorySpending_ShouldGroupAndOrderDescending_ExcludingNonMatching() {
        // Arrange
        User owner = newUser();
        User otherUser = newUser();
        Category rent = newCategory(owner, "Rent", TransactionType.EXPENSE);
        Category food = newCategory(owner, "Food", TransactionType.EXPENSE);
        Category salary = newCategory(owner, "Salary", TransactionType.INCOME);

        // Rent = 50 + 25 = 75 ; Food = 100 -> Food must rank first
        saveNow(owner, rent, TransactionType.EXPENSE, "50.00", true);
        saveNow(owner, rent, TransactionType.EXPENSE, "25.00", true);
        saveNow(owner, food, TransactionType.EXPENSE, "100.00", true);
        // Excluded rows
        saveNow(owner, salary, TransactionType.INCOME, "999.00", true);   // income
        saveNow(owner, rent, TransactionType.EXPENSE, "999.00", false);   // inactive
        Category otherCat = newCategory(otherUser, "Other", TransactionType.EXPENSE);
        saveNow(otherUser, otherCat, TransactionType.EXPENSE, "999.00", true); // other user

        // Act
        List<CategorySpendingProjection> rows = transactionRepository
                .findCategorySpending(owner.getId(), null, null, Pageable.unpaged());

        // Assert
        assertEquals(2, rows.size(), "Only the owner's two active expense categories should appear");
        assertEquals("Food", rows.get(0).getCategoryName(), "Highest spend (100) must rank first");
        assertEquals(0, new BigDecimal("100.00").compareTo(rows.get(0).getTotalSpent()));
        assertEquals("Rent", rows.get(1).getCategoryName());
        assertEquals(0, new BigDecimal("75.00").compareTo(rows.get(1).getTotalSpent()),
                "Rent total must sum its two active rows (50 + 25)");
    }

    @Test
    @DisplayName("Should return only the top category when a single-element page is requested")
    void findCategorySpending_ShouldRespectPageable_TopCategoryOnly() {
        // Arrange
        User owner = newUser();
        Category rent = newCategory(owner, "Rent", TransactionType.EXPENSE);
        Category food = newCategory(owner, "Food", TransactionType.EXPENSE);
        saveNow(owner, rent, TransactionType.EXPENSE, "75.00", true);
        saveNow(owner, food, TransactionType.EXPENSE, "100.00", true);

        // Act
        List<CategorySpendingProjection> rows = transactionRepository
                .findCategorySpending(owner.getId(), null, null, PageRequest.of(0, 1));

        // Assert
        assertEquals(1, rows.size(), "A page size of 1 must cap the result at the single top category");
        assertEquals("Food", rows.getFirst().getCategoryName());
    }

    @Test
    @DisplayName("Should exclude expenses created before the start of the requested window")
    void findCategorySpending_ShouldFilterByStartDate() {
        // Arrange
        User owner = newUser();
        Category rent = newCategory(owner, "Rent", TransactionType.EXPENSE);

        Instant startOfThisMonth = LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant lastMonth = startOfThisMonth.minus(Duration.ofDays(5));

        saveNow(owner, rent, TransactionType.EXPENSE, "60.00", true);              // in window
        insertAt(owner, rent, TransactionType.EXPENSE, "40.00", true, lastMonth);  // before window

        // Act
        List<CategorySpendingProjection> rows = transactionRepository
                .findCategorySpending(owner.getId(), startOfThisMonth, null, Pageable.unpaged());

        // Assert
        assertEquals(1, rows.size());
        assertEquals(0, new BigDecimal("60.00").compareTo(rows.getFirst().getTotalSpent()),
                "Only the in-window expense (60) should be counted; the back-dated 40 is excluded");
    }
    //</editor-fold>

    //<editor-fold desc="Test calculatePeriodTotals">
    @Test
    @DisplayName("Should split income and expense totals and count only active transactions")
    void calculatePeriodTotals_ShouldComputeTotalsAndCount_ExcludingInactive() {
        // Arrange
        User owner = newUser();
        Category salary = newCategory(owner, "Salary", TransactionType.INCOME);
        Category rent = newCategory(owner, "Rent", TransactionType.EXPENSE);

        saveNow(owner, salary, TransactionType.INCOME, "120.00", true);
        saveNow(owner, salary, TransactionType.INCOME, "80.00", true);   // income total = 200
        saveNow(owner, rent, TransactionType.EXPENSE, "50.00", true);    // expense total = 50
        saveNow(owner, salary, TransactionType.INCOME, "999.00", false); // inactive -> excluded

        // Act
        PeriodTotalsProjection totals = transactionRepository
                .calculatePeriodTotals(owner.getId(), null, null);

        // Assert
        assertEquals(0, new BigDecimal("200.00").compareTo(totals.getTotalIncome()));
        assertEquals(0, new BigDecimal("50.00").compareTo(totals.getTotalExpense()));
        assertEquals(3L, totals.getTransactionCount(), "Only the three active rows should be counted");
    }

    @Test
    @DisplayName("Should return zeroed totals and a zero count when the user has no transactions")
    void calculatePeriodTotals_ShouldReturnZeros_WhenNoTransactions() {
        // Arrange
        User owner = newUser();

        // Act
        PeriodTotalsProjection totals = transactionRepository
                .calculatePeriodTotals(owner.getId(), null, null);

        // Assert: COALESCE guarantees non-null zero totals
        assertEquals(0, BigDecimal.ZERO.compareTo(totals.getTotalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(totals.getTotalExpense()));
        assertEquals(0L, totals.getTransactionCount());
    }

    @Test
    @DisplayName("Should only total transactions that fall inside the requested window")
    void calculatePeriodTotals_ShouldFilterByDateRange() {
        // Arrange
        User owner = newUser();
        Category salary = newCategory(owner, "Salary", TransactionType.INCOME);

        Instant startOfThisMonth = LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant lastMonth = startOfThisMonth.minus(Duration.ofDays(5));

        saveNow(owner, salary, TransactionType.INCOME, "200.00", true);            // in window
        insertAt(owner, salary, TransactionType.INCOME, "500.00", true, lastMonth); // before window

        // Act
        PeriodTotalsProjection totals = transactionRepository
                .calculatePeriodTotals(owner.getId(), startOfThisMonth, null);

        // Assert
        assertEquals(0, new BigDecimal("200.00").compareTo(totals.getTotalIncome()),
                "The back-dated 500 income must be excluded by the start bound");
        assertEquals(1L, totals.getTransactionCount());
    }
    //</editor-fold>

    //<editor-fold desc="Test findMonthlyTrend">
    @Test
    @DisplayName("Should bucket amounts by month and type, ordered chronologically, excluding inactive and other users")
    void findMonthlyTrend_ShouldGroupByMonthAndType_OrderedChronologically() {
        // Arrange
        User owner = newUser();
        User otherUser = newUser();
        Category salary = newCategory(owner, "Salary", TransactionType.INCOME);
        Category rent = newCategory(owner, "Rent", TransactionType.EXPENSE);

        ZonedDateTime nowUtc = Instant.now().atZone(ZoneOffset.UTC);
        Instant monthMinus2 = nowUtc.minusMonths(2).toInstant();
        Instant monthMinus1 = nowUtc.minusMonths(1).toInstant();

        int currentKey = nowUtc.getYear() * 100 + nowUtc.getMonthValue();

        insertAt(owner, salary, TransactionType.INCOME, "100.00", true, monthMinus2);
        insertAt(owner, rent, TransactionType.EXPENSE, "50.00", true, monthMinus1);
        saveNow(owner, salary, TransactionType.INCOME, "200.00", true);   // current month income
        saveNow(owner, rent, TransactionType.EXPENSE, "30.00", true);     // current month expense
        saveNow(owner, rent, TransactionType.EXPENSE, "999.00", false);   // inactive -> excluded
        Category otherSalary = newCategory(otherUser, "OtherSalary", TransactionType.INCOME);
        saveNow(otherUser, otherSalary, TransactionType.INCOME, "999.00", true); // other user -> excluded

        // Act
        List<MonthlyTrendProjection> rows = transactionRepository
                .findMonthlyTrend(owner.getId(), null, null);

        // Assert: 4 buckets (2 historical single-type + 2 current-month types)
        assertEquals(4, rows.size(), "Inactive and other-user rows must not produce buckets");

        // Chronological ordering: year*100+month must be non-decreasing
        List<Integer> keys = rows.stream().map(r -> r.getYear() * 100 + r.getMonth()).toList();
        List<Integer> sorted = keys.stream().sorted().toList();
        assertEquals(sorted, keys, "Rows must be returned in chronological order");

        // Current-month EXPENSE bucket must be 30 (proves the inactive 999 was excluded)
        BigDecimal currentExpense = rows.stream()
                .filter(r -> r.getYear() * 100 + r.getMonth() == currentKey
                        && r.getTransactionType() == TransactionType.EXPENSE)
                .map(MonthlyTrendProjection::getTotalAmount)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("30.00").compareTo(currentExpense));

        BigDecimal currentIncome = rows.stream()
                .filter(r -> r.getYear() * 100 + r.getMonth() == currentKey
                        && r.getTransactionType() == TransactionType.INCOME)
                .map(MonthlyTrendProjection::getTotalAmount)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(currentIncome));
    }

    @Test
    @DisplayName("Should return an empty trend when the user has no transactions")
    void findMonthlyTrend_ShouldReturnEmpty_WhenNoTransactions() {
        // Arrange
        User owner = newUser();

        // Act
        List<MonthlyTrendProjection> rows = transactionRepository
                .findMonthlyTrend(owner.getId(), null, null);

        // Assert
        assertTrue(rows.isEmpty(), "A user with no transactions must yield an empty trend");
    }
    //</editor-fold>
}
