package com.seap.smartfinancetracker.common.seeder;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.user.entity.User;
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

@Slf4j
@Component
@Profile({"local", "dev"})
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
            User newUser = new User();
            newUser.setEmail(TEST_USER_EMAIL);
            newUser.setPassword(passwordEncoder.encode("password"));
            newUser.setFullName("Test User");
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
                Transaction tx = new Transaction();
                tx.setUser(testUser); // Attach the test user
                tx.setActive(true);
                tx.setOverBudget(false);
                tx.setIdempotencyKey(UUID.randomUUID());

                // Randomize date within the last 60 days
                Instant randomDate = Instant.now().minus(faker.number().numberBetween(0, 60), ChronoUnit.DAYS);
                tx.setCreatedAt(randomDate);

                // 80% chance of being an EXPENSE, 20% INCOME
                if (faker.number().numberBetween(1, 100) <= 80) {
                    tx.setTransactionType(TransactionType.EXPENSE);
                    tx.setCategory(expenseCategories.get(faker.number().numberBetween(0, expenseCategories.size())));
                    // Random amount between 10,000 and 1,000,000
                    tx.setAmount(BigDecimal.valueOf(faker.number().randomDouble(0, 10000, 1000000)));
                    tx.setNote(faker.food().dish() + " with friends");
                } else {
                    tx.setTransactionType(TransactionType.INCOME);
                    tx.setCategory(incomeCategories.get(faker.number().numberBetween(0, incomeCategories.size())));
                    // Random amount between 5,000,000 and 30,000,000
                    tx.setAmount(BigDecimal.valueOf(faker.number().randomDouble(0, 5000000, 30000000)));
                    tx.setNote("Monthly Salary or Bonus");
                }

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
