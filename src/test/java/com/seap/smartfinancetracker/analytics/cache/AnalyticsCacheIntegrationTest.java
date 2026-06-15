package com.seap.smartfinancetracker.analytics.cache;

import com.seap.smartfinancetracker.analytics.dto.AnalyticsPeriodRequest;
import com.seap.smartfinancetracker.analytics.dto.PeriodSummaryResponse;
import com.seap.smartfinancetracker.analytics.dto.SpendingByCategoryResponse;
import com.seap.smartfinancetracker.analytics.service.AnalyticsService;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.transaction.dto.TransactionCreateRequest;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
import com.seap.smartfinancetracker.transaction.service.TransactionService;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

/**
 * Integration tests for the analytics caching layer.
 * <p>
 * Backed by real Postgres and Redis containers, these tests exercise the full
 * {@code @Cacheable} + version-stamp + eviction-aspect machinery: a populated cache serves the
 * stored value even when the database changes underneath it, a transaction mutation routed
 * through {@link TransactionService} invalidates the cache, one user's invalidation never
 * affects another's entries, and {@code Instant}-bounded windows round-trip through Redis.
 * </p>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AnalyticsCacheIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static final AnalyticsPeriodRequest UNBOUNDED = new AnalyticsPeriodRequest(null, null);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnalyticsCacheService analyticsCacheService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Kafka is irrelevant to caching; mock the template so createTransaction needs no broker.
    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private User user;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        flushRedis();

        user = saveUser();
        expenseCategory = saveCategory(user, "Rent", TransactionType.EXPENSE);
    }

    private void flushRedis() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    private User saveUser() {
        return userRepository.save(Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create());
    }

    private Category saveCategory(User owner, String name, TransactionType type) {
        return categoryRepository.save(Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), owner)
                .set(field(Category::getCategoryName), name)
                .set(field(Category::getTransactionType), type)
                .set(field(Category::isActive), true)
                .create());
    }

    private void saveExpense(User owner, Category category, String amount) {
        transactionRepository.save(Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), owner)
                .set(field(Transaction::getCategory), category)
                .set(field(Transaction::getTransactionType), TransactionType.EXPENSE)
                .set(field(Transaction::getAmount), new BigDecimal(amount))
                .set(field(Transaction::isActive), true)
                .set(field(Transaction::getIdempotencyKey), UUID.randomUUID())
                .create());
    }
    //</editor-fold>

    @Test
    @DisplayName("Should serve the cached value even after the database changes, until invalidated")
    void shouldServeCachedValueUntilInvalidated() {
        saveExpense(user, expenseCategory, "100.00");

        // First call populates the cache.
        SpendingByCategoryResponse first = analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED);
        assertThat(first.totalExpense()).isEqualByComparingTo("100.00");

        // Change the data behind the cache's back (no eviction is triggered).
        saveExpense(user, expenseCategory, "50.00");

        // The stale cached value is still served.
        SpendingByCategoryResponse cached = analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED);
        assertThat(cached.totalExpense()).isEqualByComparingTo("100.00");

        // After an explicit invalidation, the next read recomputes from the database.
        analyticsCacheService.invalidate(user.getId());
        SpendingByCategoryResponse fresh = analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED);
        assertThat(fresh.totalExpense()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Should evict the cache when a transaction is created through the service")
    void shouldEvictCacheWhenTransactionCreatedThroughService() {
        saveExpense(user, expenseCategory, "100.00");

        SpendingByCategoryResponse before = analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED);
        assertThat(before.totalExpense()).isEqualByComparingTo("100.00");

        // A create routed through the service must fire the eviction aspect after commit.
        transactionService.createTransaction(user.getId(), TransactionCreateRequest.builder()
                .categoryId(expenseCategory.getId())
                .transactionType(TransactionType.EXPENSE)
                .amount(new BigDecimal("30.00"))
                .build());

        SpendingByCategoryResponse after = analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED);
        assertThat(after.totalExpense()).isEqualByComparingTo("130.00");
    }

    @Test
    @DisplayName("Should not evict another user's cache when one user is invalidated")
    void shouldNotEvictOtherUsersCache() {
        User otherUser = saveUser();
        Category otherCategory = saveCategory(otherUser, "Food", TransactionType.EXPENSE);

        saveExpense(user, expenseCategory, "100.00");
        saveExpense(otherUser, otherCategory, "200.00");

        // Populate both users' caches.
        assertThat(analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED).totalExpense())
                .isEqualByComparingTo("100.00");
        assertThat(analyticsService.getSpendingByCategory(otherUser.getId(), UNBOUNDED).totalExpense())
                .isEqualByComparingTo("200.00");

        // Mutate both users' data behind the cache, then invalidate only the first user.
        saveExpense(user, expenseCategory, "10.00");
        saveExpense(otherUser, otherCategory, "20.00");
        analyticsCacheService.invalidate(user.getId());

        // The first user recomputes; the second still serves its (untouched) cached value.
        assertThat(analyticsService.getSpendingByCategory(user.getId(), UNBOUNDED).totalExpense())
                .isEqualByComparingTo("110.00");
        assertThat(analyticsService.getSpendingByCategory(otherUser.getId(), UNBOUNDED).totalExpense())
                .isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Should round-trip an Instant-bounded summary through Redis")
    void shouldRoundTripInstantBoundedSummaryThroughRedis() {
        saveExpense(user, expenseCategory, "100.00");

        AnalyticsPeriodRequest window = new AnalyticsPeriodRequest(
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(3600));

        // First call serializes the response (Instant bounds included) into Redis.
        PeriodSummaryResponse first = analyticsService.getPeriodSummary(user.getId(), window);
        assertThat(first.totalExpense()).isEqualByComparingTo("100.00");
        assertThat(first.startDate()).isEqualTo(window.startDate());
        assertThat(first.endDate()).isEqualTo(window.endDate());

        // A successful second read proves the value (with its Instants) deserialized cleanly:
        // it is served from cache, so the database change below is not reflected.
        saveExpense(user, expenseCategory, "50.00");
        PeriodSummaryResponse cached = analyticsService.getPeriodSummary(user.getId(), window);
        assertThat(cached.totalExpense()).isEqualByComparingTo("100.00");
        assertThat(cached.startDate()).isEqualTo(window.startDate());
        assertThat(cached.endDate()).isEqualTo(window.endDate());
    }
}
