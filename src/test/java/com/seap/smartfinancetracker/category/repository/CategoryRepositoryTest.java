package com.seap.smartfinancetracker.category.repository;

import com.seap.smartfinancetracker.category.entity.Category;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private  CategoryRepository categoryRepository;

    @Test
    @DisplayName("Should return categories strictly belonging to the given user ID")
    void findByUserId_ShouldReturnOnlyUserCategories() {
        // Arrange: Create and persist a target user and another user
        User targetUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();

        targetUser = entityManager.persistAndFlush(targetUser);

        User otherUser = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        otherUser = entityManager.persistAndFlush(otherUser);

        // Arrange: Create categories for both users
        Category targetUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), targetUser)
                .ignore(Select.field(Category::getId))
                .create();

        Category otherUserCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), otherUser)
                .ignore(Select.field(Category::getId))
                .create();

        categoryRepository.saveAll(List.of(targetUserCategory, otherUserCategory));

        // Act: Fetch categories by the target user's ID
        List<Category> results = categoryRepository.findByUserId(targetUser.getId());

        // Assert: Verify only the target user's category is returned
        assertEquals(1, results.size(), "Should find exactly 1 category for the target user");
        assertEquals(targetUser.getId(), results.getFirst().getUser().getId(), "Returned category must belong to the target user");
    }

    @Test
    @DisplayName("Should return default/system categories where user ID is null")
    void findByUserIdIsNull_ShouldReturnSystemDefaultCategories() {
        // Arrange: Create a normal user
        User user = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();
        user = entityManager.persistAndFlush(user);

        // Arrange: Create a user-specific category
        Category personalCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), user)
                .ignore(Select.field(Category::getId))
                .create();

        categoryRepository.saveAll(List.of(personalCategory));

        // Act: Fetch categories with null user ID
        List<Category> results = categoryRepository.findByUserIdIsNull();

        // Assert: Verify only the system category is returned
        assertEquals(2, results.size(), "Should find exactly 3 system default categoríes");
        assertNull(results.getFirst().getUser(), "The user field of the returned category must be null");
    }

    @Test
    @DisplayName("Should return the category when both Category ID and User ID match securely")
    void findByIdAndUserId_ShouldReturnCategory_WhenOwnedByGivenUser() {
        // Arrange: Create the owner user
        User owner = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();

        owner = entityManager.persistAndFlush(owner);

        // Arrange: Create a category belonging to the owner
        Category category = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), owner)
                .ignore(Select.field(Category::getId))
                .create();
        category = categoryRepository.save(category);

        // Act: Attempt to fetch the category with correct Category ID and Owner ID
        Optional<Category> result = categoryRepository.findByIdAndUserId(category.getId(), owner.getId());

        // Assert: The category should be present
        assertTrue(result.isPresent(), "Category should be found when requested by its rightful owner");
        assertEquals(category.getId(), result.get().getId(), "The IDs must match exactly");
    }

    @Test
    @DisplayName("Should return empty Optional when a user tries to access another user's category (IDOR prevention)")
    void findByIdAndUserId_ShouldReturnEmpty_WhenNotOwnedByGivenUser() {
        // Arrange: Create the true owner and an attacker/other user
        User owner = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();

        owner = entityManager.persistAndFlush(owner);

        User attacker = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .create();

        attacker = entityManager.persistAndFlush(attacker);

        // Arrange: Create a category belonging to the true owner
        Category ownerCategory = Instancio.of(Category.class)
                .set(Select.field(Category::getUser), owner)
                .ignore(Select.field(Category::getId))
                .create();

        ownerCategory = categoryRepository.save(ownerCategory);

        // Act: The attacker tries to fetch the owner's category using the attacker's own User ID
        Optional<Category> result = categoryRepository.findByIdAndUserId(ownerCategory.getId(), attacker.getId());

        // Assert: The result must be empty to prevent unauthorized access
        assertTrue(result.isEmpty(), "Category should NOT be found when requested by a different user");
    }
}
