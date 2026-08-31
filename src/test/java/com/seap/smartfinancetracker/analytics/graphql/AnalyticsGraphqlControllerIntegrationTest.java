package com.seap.smartfinancetracker.analytics.graphql;

import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.mapper.UserPrincipalMapper;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.entity.Transaction;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.transaction.repository.TransactionRepository;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link AnalyticsGraphqlController}.
 * <p>
 * Drives the full HTTP → security → GraphQL → service → repository stack against a real
 * Postgres (Testcontainers), POSTing GraphQL documents to {@code /graphql} with a JWT. Mirrors
 * the REST analytics integration test but exercises the nested {@code analytics} query and its
 * lazily-resolved sub-fields.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AnalyticsGraphqlControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserPrincipalMapper userPrincipalMapper;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;
    private Category rentCategory;
    private Category foodCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create());

        rentCategory = saveCategory("Rent", TransactionType.EXPENSE);
        foodCategory = saveCategory("Food", TransactionType.EXPENSE);
        salaryCategory = saveCategory("Salary", TransactionType.INCOME);

        UserPrincipal userPrincipal = userPrincipalMapper.toUserPrincipal(testUser);
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }

    private Category saveCategory(String name, TransactionType type) {
        return categoryRepository.save(Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::getCategoryName), name)
                .set(field(Category::getTransactionType), type)
                .set(field(Category::isActive), true)
                .create());
    }

    private void saveTransaction(Category category, TransactionType type, String amount) {
        transactionRepository.save(Instancio.of(Transaction.class)
                .ignore(field(Transaction::getId))
                .set(field(Transaction::getUser), testUser)
                .set(field(Transaction::getCategory), category)
                .set(field(Transaction::getTransactionType), type)
                .set(field(Transaction::getAmount), new BigDecimal(amount))
                .set(field(Transaction::isActive), true)
                .set(field(Transaction::getIdempotencyKey), UUID.randomUUID())
                .create());
    }

    /**
     * POSTs a GraphQL document to the endpoint with the given bearer token (or none).
     */
    private org.springframework.test.web.servlet.ResultActions perform(String query, String token) throws Exception {
        var request = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"" + query.replace("\"", "\\\"") + "\"}");
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }
    //</editor-fold>

    //<editor-fold desc="Selective field resolution">
    @Test
    @DisplayName("Should resolve only the summary section when only summary is selected")
    void shouldResolveSummaryOnly() throws Exception {
        // Arrange: income 200; expenses Rent 75 + Food 25 = 100 -> net 100, count 3
        saveTransaction(salaryCategory, TransactionType.INCOME, "200.00");
        saveTransaction(rentCategory, TransactionType.EXPENSE, "75.00");
        saveTransaction(foodCategory, TransactionType.EXPENSE, "25.00");

        String query = "{ analytics { summary { totalIncome totalExpense net transactionCount topCategoryName topCategoryAmount } } }";

        perform(query, validToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.analytics.summary.totalIncome").value(200.00))
                .andExpect(jsonPath("$.data.analytics.summary.totalExpense").value(100.00))
                .andExpect(jsonPath("$.data.analytics.summary.net").value(100.00))
                .andExpect(jsonPath("$.data.analytics.summary.transactionCount").value(3))
                .andExpect(jsonPath("$.data.analytics.summary.topCategoryName").value("Rent"))
                .andExpect(jsonPath("$.data.analytics.summary.topCategoryAmount").value(75.00))
                // spendingByCategory was not selected, so it must be absent from the response
                .andExpect(jsonPath("$.data.analytics.spendingByCategory").doesNotExist());
    }

    @Test
    @DisplayName("Should resolve all three sections in a single request")
    void shouldResolveAllSections() throws Exception {
        // Arrange: Rent 75, Food 25 -> total 100 (75% / 25%); income 200
        saveTransaction(salaryCategory, TransactionType.INCOME, "200.00");
        saveTransaction(rentCategory, TransactionType.EXPENSE, "75.00");
        saveTransaction(foodCategory, TransactionType.EXPENSE, "25.00");

        String query = "{ analytics { "
                + "summary { net } "
                + "spendingByCategory { totalExpense categories { categoryName percentage } } "
                + "incomeExpenseTrend { totalIncome totalExpense net } "
                + "} }";

        ResultActions resultActions = perform(query, validToken);
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.analytics.summary.net").value(100.00))
                .andExpect(jsonPath("$.data.analytics.spendingByCategory.totalExpense").value(100.00))
                .andExpect(jsonPath("$.data.analytics.spendingByCategory.categories[0].categoryName").value("Rent"))
                .andExpect(jsonPath("$.data.analytics.spendingByCategory.categories[0].percentage").value(75.0))
                .andExpect(jsonPath("$.data.analytics.incomeExpenseTrend[0].net").value(100.00));
    }
    //</editor-fold>

    //<editor-fold desc="Validation & Security">
    @Test
    @DisplayName("Should surface the ANL-400-01 code in error extensions for an inverted range")
    void shouldSurfaceBusinessErrorCode() throws Exception {
        // Raw double-quotes here; perform() escapes them for the JSON envelope.
        String query = "{ analytics(startDate: \"2026-06-10T00:00:00Z\", endDate: \"2026-06-01T00:00:00Z\") { summary { net } } }";

        perform(query, validToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analytics").doesNotExist())
                .andExpect(jsonPath("$.errors[0].extensions.code").value("ANL-400-01"));
    }

    @Test
    @DisplayName("Should reject an unauthenticated GraphQL request")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        String query = "{ analytics { summary { net } } }";

        perform(query, null)
                .andExpect(status().is4xxClientError());
    }
    //</editor-fold>
}
