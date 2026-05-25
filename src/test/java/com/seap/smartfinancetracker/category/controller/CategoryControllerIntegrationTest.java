package com.seap.smartfinancetracker.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.transaction.enums.TransactionType;
import com.seap.smartfinancetracker.user.entity.User;
import com.seap.smartfinancetracker.user.enums.Role;
import com.seap.smartfinancetracker.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EnableJpaAuditing
@ActiveProfiles("test")
public class CategoryControllerIntegrationTest {

    //<editor-fold desc="Setup & Configurations">
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtService jwtService;

    private String validToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = Instancio.of(User.class)
                .ignore(field(User::getId))
                .set(field(User::getRole), Role.USER)
                .create();

        testUser = userRepository.save(testUser);

        // Create JWT Test Token
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(testUser.getId())
                .email(testUser.getEmail())
                .build();
        validToken = jwtService.generateToken(new HashMap<>(), userPrincipal);
    }
    //</editor-fold>

    //<editor-fold desc="POST /categories">
    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        CategoryCreateRequest request = Instancio.of(CategoryCreateRequest.class)
                .set(field(CategoryCreateRequest::transactionType), TransactionType.EXPENSE)
                .create();

        // Act & Assert
        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Verify the response to match with request data
                .andExpect(jsonPath("$.categoryName").value(request.categoryName()))
                .andExpect(jsonPath("$.transactionType").value(request.transactionType().name()));
    }

    @Test
    void shouldFailToCreateCategory_WhenNoTokenProvided() throws Exception {
        // Arrange: Create request with random data
        CategoryCreateRequest request = Instancio.create(CategoryCreateRequest.class);

        // Act & Assert
        // No Token provide
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
    //</editor-fold>

    //<editor-fold desc="GET /categories">
    @Test
    void shouldGetAllCategoriesForUser() throws Exception {
        // Arrange: Gen request
        CategoryCreateRequest request = Instancio.of(CategoryCreateRequest.class)
                .set(field(CategoryCreateRequest::transactionType), TransactionType.INCOME)
                .create();

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(get("/categories")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].categoryName", hasItem(request.categoryName())))
                .andExpect(jsonPath("$[*].transactionType", hasItem(request.transactionType().name())));
    }
    //</editor-fold>

    //<editor-fold desc="GET /categories/{categoryId} (Get 1 Category)">
    @Test
    void shouldGetCategoryByIdSuccessfully() throws Exception {
        // Arrange: Generate and save a category directly into DB for the current user
        // Note: Assuming your Entity is named 'Category' and has 'userId' field
        Category category = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::isActive), true)
                .create();
        category = categoryRepository.save(category);

        // Act & Assert: Call the endpoint and verify response
        mockMvc.perform(get("/categories/{categoryId}", category.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                // Verify the returned ID matches
                .andExpect(jsonPath("$.id").value(category.getId().toString()))
                // Verify the data integrity matches Instancio's generated data
                .andExpect(jsonPath("$.categoryName").value(category.getCategoryName()))
                .andExpect(jsonPath("$.transactionType").value(category.getTransactionType().name()));
    }

    @Test
    void shouldReturnError_WhenCategoryNotFound() throws Exception {
        // Arrange: Create a random UUID that definitely doesn't exist in DB
        UUID fakeCategoryId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/categories/{categoryId}", fakeCategoryId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }
    //</editor-fold>

    //<editor-fold desc="DELETE /categories/{categoryId} (Deactivate Category)">
    @Test
    void shouldDeactivateCategorySuccessfully() throws Exception {
        // Arrange: Create an active category in DB
        Category category = Instancio.of(Category.class)
                .ignore(field(Category::getId))
                .set(field(Category::getUser), testUser)
                .set(field(Category::isActive), true)
                .create();
        category = categoryRepository.save(category);

        // Act & Assert: Call DELETE endpoint
        mockMvc.perform(delete("/categories/{categoryId}", category.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent()); // Expecting 204 No Content

        // Bonus Assert: Verify the database actually updated the 'active' flag to false
        Category updatedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertFalse(updatedCategory.isActive(), "The category should be deactivated (active = false)");
    }
    //</editor-fold>
}
