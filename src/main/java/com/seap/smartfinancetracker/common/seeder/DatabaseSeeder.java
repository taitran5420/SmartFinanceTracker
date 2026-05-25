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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the database with initial test data in local and development profiles.
 *
 * <p>This runner creates a default test user and generates sample transactions
 * if the database is empty. It is not executed in production.</p>
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

    private static final String TEST_USER_EMAIL = "test@smartfinance.com";

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
        if (transactionRepository.count() == 0) {
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

            // Generate 100 random transactions
            List<Transaction> transactions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Instant randomDate = Instant.now().minus(faker.number().numberBetween(0, 60), ChronoUnit.DAYS);

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

                Transaction tx = Transaction.builder()
                        .user(testUser)
                        .active(true)
                        .idempotencyKey(UUID.randomUUID())
                        .createdAt(randomDate)
                        .transactionType(type)
                        .category(category)
                        .amount(amount)
                        .note(note)
                        .build();

                transactions.add(tx);

                transactions.add(tx);
            }

            // Batch save for better performance
            transactionRepository.saveAll(transactions);
            log.info("Successfully seeded {} fake transactions!", transactions.size());
            log.info("--- SEEDING COMPLETE. YOU CAN LOG IN WITH EMAIL: {} ---", TEST_USER_EMAIL);
        } else {
            log.info("Database already contains transactions. Skipping transaction seeding.");
        }
    }
}
