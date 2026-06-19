package com.seap.smartfinancetracker.common.seeder;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the database with initial test data in local and development profiles.
 *
 * <p>This runner creates a default test user and generates sample transactions
 * if the database is empty. It is not executed in production.</p>
 *
 * <p><b>Why a native insert?</b> {@link Transaction#getCreatedAt()} is a
 * {@code @CreatedDate} field managed by Spring Data JPA auditing, which overwrites any
 * value supplied through the entity builder with the current timestamp on persist. To seed
 * transactions across a spread of historical dates, the rows are inserted directly via
 * {@link JdbcTemplate}, bypassing the auditing listener. This is safe because the
 * {@code trg_prevent_transaction_updates} immutability trigger only guards {@code UPDATE}s,
 * not {@code INSERT}s.</p>
 */
@Slf4j
@Component
@Profile({"local"})
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String TEST_USER_EMAIL = "test@smartfinance.com";

    private static final int TRANSACTION_COUNT = 100;
    private static final int MAX_DAYS_IN_PAST = 60;

    /**
     * Inserts transactions with an explicit {@code created_at}, casting the enum column and
     * binding the timestamp as {@code timestamptz}. {@code updated_at} and {@code is_over_budget}
     * are populated explicitly rather than relying on column defaults.
     */
    private static final String INSERT_TRANSACTION_SQL = """
            INSERT INTO transactions
                (id, user_id, category_id, amount, transaction_type, note,
                 idempotency_key, created_at, updated_at, active, is_over_budget)
            VALUES
                (?, ?, ?, ?, CAST(? AS transaction_type_enum), ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void run(String @NonNull ... args) {
        log.info("Checking if database seeding is required...");

        // 1. Seed Test User (If not exists)
        User testUser = userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() -> {
            log.info("Test user not found. Creating a new test user...");
            User newUser = User.builder()
                    .email(TEST_USER_EMAIL)
                    .password(passwordEncoder.encode("password"))
                    .fullName("Test User")
                    .role(Role.USER)
                    .build();
            return userRepository.save(newUser);
        });

        // 2. Seed Transactions (Only if the table is empty to avoid duplicating data on restarts)
        if (transactionRepository.count() != 0) {
            log.info("Database already contains transactions. Skipping transaction seeding.");
            return;
        }

        log.info("Transaction table is empty. Generating fake transactions for user: {}", TEST_USER_EMAIL);

        Faker faker = new Faker();

        // Fetch default categories created by Flyway
        List<Category> expenseCategories = categoryRepository.findAll().stream()
                .filter(c -> c.getTransactionType() == TransactionType.EXPENSE)
                .toList();

        List<Category> incomeCategories = categoryRepository.findAll().stream()
                .filter(c -> c.getTransactionType() == TransactionType.INCOME)
                .toList();

        if (expenseCategories.isEmpty() || incomeCategories.isEmpty()) {
            log.warn("System Default Categories are missing! Please run Flyway migrations first.");
            return;
        }

        // Generate random transactions spread across the past MAX_DAYS_IN_PAST days
        List<Transaction> transactions = new ArrayList<>(TRANSACTION_COUNT);
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            Instant randomDate = Instant.now()
                    .minus(faker.number().numberBetween(0, MAX_DAYS_IN_PAST), ChronoUnit.DAYS);

            TransactionType type;
            Category category;
            BigDecimal amount;
            String note;

            // 80% chance of being an EXPENSE, 20% INCOME
            if (faker.number().numberBetween(1, 100) <= 80) {
                type = TransactionType.EXPENSE;
                category = expenseCategories.get(faker.number().numberBetween(0, expenseCategories.size()));
                // Random amount between 10,000 and 1,000,000
                amount = BigDecimal.valueOf(faker.number().randomDouble(0, 10000, 1000000));
                note = faker.food().dish() + " with friends";
            } else {
                type = TransactionType.INCOME;
                category = incomeCategories.get(faker.number().numberBetween(0, incomeCategories.size()));
                // Random amount between 5,000,000 and 30,000,000
                amount = BigDecimal.valueOf(faker.number().randomDouble(0, 5000000, 30000000));
                note = "Monthly Salary or Bonus";
            }

            transactions.add(Transaction.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .active(true)
                    .idempotencyKey(UUID.randomUUID())
                    .createdAt(randomDate)
                    .transactionType(type)
                    .category(category)
                    .amount(amount)
                    .note(note)
                    .build());
        }

        batchInsert(transactions);

        log.info("Successfully seeded {} fake transactions across the past {} days!",
                transactions.size(), MAX_DAYS_IN_PAST);
        log.info("--- SEEDING COMPLETE. YOU CAN LOG IN WITH EMAIL: {} ---", TEST_USER_EMAIL);
    }

    /**
     * Persists the given transactions via a single JDBC batch, bypassing JPA auditing so the
     * builder-supplied {@code createdAt} values are honoured.
     *
     * @param transactions the fully-populated, not-yet-persisted transactions to insert
     */
    private void batchInsert(List<Transaction> transactions) {
        jdbcTemplate.batchUpdate(INSERT_TRANSACTION_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                Transaction tx = transactions.get(i);
                ps.setObject(1, tx.getId());
                ps.setObject(2, tx.getUser().getId());
                ps.setObject(3, tx.getCategory().getId());
                ps.setBigDecimal(4, tx.getAmount());
                ps.setString(5, tx.getTransactionType().name());
                ps.setString(6, tx.getNote());
                ps.setObject(7, tx.getIdempotencyKey());
                // timestamptz binds cleanly from OffsetDateTime; the pg driver rejects a bare Instant.
                ps.setObject(8, tx.getCreatedAt().atOffset(ZoneOffset.UTC));
                ps.setObject(9, tx.getCreatedAt().atOffset(ZoneOffset.UTC));
                ps.setBoolean(10, tx.isActive());
                ps.setBoolean(11, tx.isOverBudget());
            }

            @Override
            public int getBatchSize() {
                return transactions.size();
            }
        });
    }
}
