package com.seap.smartfinancetracker.transaction.repository;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.transaction.entity.RecurringTransaction;
import com.seap.smartfinancetracker.transaction.enums.Frequency;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecurringTransactionRepositoryTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;
    //</editor-fold>

    //<editor-fold desc="Test findByIdAndUserId">
    @Test
    @DisplayName("Should return recurring transaction when ID and User ID match")
    void findByIdAndUserId_ShouldReturnTransaction() {
        // Arrange
        User owner = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        User attacker = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());

        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class)
                        .set(Select.field(Category::getUser), owner)
                        .ignore(Select.field(Category::getId))
                        .create()
        );

        RecurringTransaction transaction = entityManager.persistAndFlush(
                Instancio.of(RecurringTransaction.class)
                        .set(Select.field(RecurringTransaction::getUser), owner)
                        .set(Select.field(RecurringTransaction::getCategory), category)
                        .ignore(Select.field(RecurringTransaction::getId))
                        .ignore(Select.field(RecurringTransaction::getCreatedAt))
                        .ignore(Select.field(RecurringTransaction::getUpdatedAt))
                        .create()
        );

        // Act & Assert 1: Success path
        Optional<RecurringTransaction> result = recurringTransactionRepository.findByIdAndUserId(transaction.getId(), owner.getId());
        assertTrue(result.isPresent(), "Transaction should be found for the legitimate owner");

        // Act & Assert 2: Failure path (IDOR prevention)
        Optional<RecurringTransaction> notFoundResult = recurringTransactionRepository.findByIdAndUserId(transaction.getId(), attacker.getId());
        assertTrue(notFoundResult.isEmpty(), "Transaction should NOT be found for a different user");
    }
    //</editor-fold>

    //<editor-fold desc="Test findUpComingTransactions">
    @Test
    @DisplayName("Should return active transactions within the next 7 days, ordered correctly")
    void findUpComingTransactions_ShouldReturnActiveAndWithinDateRange() {
        // Arrange
        User user = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class).set(Select.field(Category::getUser), user).ignore(Select.field(Category::getId)).create()
        );

        LocalDate today = LocalDate.now();
        LocalDate next7Days = today.plusDays(7);

        // 1. Valid: Active, due in 2 days at 10:00 AM
        RecurringTransaction tx1 = createRecurringTx(user, category, true, today.plusDays(2), LocalTime.of(10, 0));

        // 2. Valid: Active, due in 2 days at 08:00 AM (Should appear BEFORE tx1 due to ORDER BY executionTime)
        RecurringTransaction tx2 = createRecurringTx(user, category, true, today.plusDays(2), LocalTime.of(8, 0));

        // 3. Invalid: Active but due in 10 days (Out of range)
        createRecurringTx(user, category, true, today.plusDays(10), LocalTime.of(9, 0));

        // 4. Invalid: Inactive but due in 3 days
        createRecurringTx(user, category, false, today.plusDays(3), LocalTime.of(9, 0));

        // Act
        List<RecurringTransaction> results = recurringTransactionRepository.findUpComingTransactions(user.getId(), today, next7Days);

        // Assert
        assertEquals(2, results.size(), "Should find exactly 2 valid upcoming transactions");

        // Verify Order: tx2 should be first because its execution time (08:00) is before tx1 (10:00) on the same date
        assertEquals(tx2.getId(), results.get(0).getId(), "tx2 must come first due to earlier execution time");
        assertEquals(tx1.getId(), results.get(1).getId(), "tx1 must come second");
    }
    //</editor-fold>

    //<editor-fold desc="Test findDueTransactions">
    @Test
    @DisplayName("Should return globally active transactions that are strictly due for processing")
    void findDueTransactions_ShouldReturnDueSchedules() {
        // Arrange
        User user = entityManager.persistAndFlush(Instancio.of(User.class).ignore(Select.field(User::getId)).create());
        Category category = entityManager.persistAndFlush(
                Instancio.of(Category.class).set(Select.field(Category::getUser), user).ignore(Select.field(Category::getId)).create()
        );

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 1. Valid: Missed execution from yesterday
        RecurringTransaction missedYesterday = createRecurringTx(user, category, true, today.minusDays(1), LocalTime.of(12, 0));

        // 2. Valid: Due today, and execution time has just passed (1 hour ago)
        RecurringTransaction dueTodayPastTime = createRecurringTx(user, category, true, today, now.minusHours(1));

        // 3. Invalid: Due today, but execution time is in the future (1 hour from now)
        createRecurringTx(user, category, true, today, now.plusHours(1));

        // 4. Invalid: Due tomorrow
        createRecurringTx(user, category, true, today.plusDays(1), LocalTime.of(12, 0));

        // 5. Invalid: Inactive, even though it was due yesterday
        createRecurringTx(user, category, false, today.minusDays(1), LocalTime.of(12, 0));

        // Act
        List<RecurringTransaction> results = recurringTransactionRepository.findDueTransactions(today, now);

        // Assert
        assertEquals(2, results.size(), "Should find exactly 2 due transactions");
        List<UUID> resultIds = results.stream().map(RecurringTransaction::getId).toList();
        assertTrue(resultIds.contains(missedYesterday.getId()), "Should include missed transaction from yesterday");
        assertTrue(resultIds.contains(dueTodayPastTime.getId()), "Should include transaction due today where time has passed");
    }
    //</editor-fold>

    //<editor-fold desc="Helper Methods">
    private RecurringTransaction createRecurringTx(User user, Category category, boolean active, LocalDate nextDate, LocalTime execTime) {
        return entityManager.persistAndFlush(
                Instancio.of(RecurringTransaction.class)
                        .set(Select.field(RecurringTransaction::getUser), user)
                        .set(Select.field(RecurringTransaction::getCategory), category)
                        .set(Select.field(RecurringTransaction::isActive), active)
                        .set(Select.field(RecurringTransaction::getNextOccurrenceDate), nextDate)
                        .set(Select.field(RecurringTransaction::getExecutionTime), execTime)
                        .set(Select.field(RecurringTransaction::getFrequency), Frequency.MONTHLY)
                        .set(Select.field(RecurringTransaction::getTransactionType), TransactionType.EXPENSE)
                        .ignore(Select.field(RecurringTransaction::getId))
                        .ignore(Select.field(RecurringTransaction::getCreatedAt))
                        .ignore(Select.field(RecurringTransaction::getUpdatedAt))
                        .create()
        );
    }
    //</editor-fold>
}