package com.seap.smartfinancetracker.category.service;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.mapper.CategoryMapper;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CategoryServiceImplTest {

    //<editor-fold desc="Setup & Configurations">
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;
    //</editor-fold>

    //<editor-fold desc="Test createCategory">
    @Test
    @DisplayName("Should successfully create a new category")
    void createCategory_ShouldReturnCategoryResponse() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        CategoryCreateRequest request = Instancio.create(CategoryCreateRequest.class);
        Category mappedEntity = Instancio.create(Category.class);
        Category savedEntity = Instancio.create(Category.class);
        CategoryResponse expectedResponse = Instancio.create(CategoryResponse.class);

        // Instruct the mocks on what to return when called
        when(categoryMapper.toEntity(userId, request)).thenReturn(mappedEntity);
        when(categoryRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(categoryMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // 2. Act
        CategoryResponse actualResponse = categoryService.createCategory(userId, request);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        // Verify that the repository's save method was called exactly once
        verify(categoryRepository, times(1)).save(mappedEntity);
    }
    //</editor-fold>

    //<editor-fold desc="Test getAllCategoriesForUser">
    @Test
    @DisplayName("Should return all user-specific and default categories")
    void getAllCategoriesForUser_ShouldReturnCombinedList() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();

        Category userCategory = Instancio.create(Category.class);
        Category defaultCategory = Instancio.create(Category.class);

        CategoryResponse userResponse = Instancio.create(CategoryResponse.class);
        CategoryResponse defaultResponse = Instancio.create(CategoryResponse.class);

        // Use new ArrayList to avoid UnsupportedOperationException if the service uses .addAll()
        when(categoryRepository.findByUserId(userId)).thenReturn(new ArrayList<>(List.of(userCategory)));
        when(categoryRepository.findByUserIdIsNull()).thenReturn(new ArrayList<>(List.of(defaultCategory)));

        when(categoryMapper.toResponse(userCategory)).thenReturn(userResponse);
        when(categoryMapper.toResponse(defaultCategory)).thenReturn(defaultResponse);

        // 2. Act
        List<CategoryResponse> results = categoryService.getAllCategoriesForUser(userId);

        // 3. Assert
        assertEquals(2, results.size(), "Should return exactly 2 categories combined");
        assertTrue(results.contains(userResponse));
        assertTrue(results.contains(defaultResponse));
    }
    //</editor-fold>

    //<editor-fold desc="Test updateCategory">
    @Test
    @DisplayName("Should successfully update an existing category")
    void updateCategory_ShouldUpdateAndReturnResponse_WhenCategoryExists() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryUpdateRequest updateRequest = Instancio.create(CategoryUpdateRequest.class);

        Category existingCategory = Instancio.create(Category.class);
        CategoryResponse expectedResponse = Instancio.create(CategoryResponse.class);

        // Note: Assuming the bug in the service is fixed and it calls (categoryId, userId) properly.
        // If you haven't fixed the bug in the service yet, you need to swap the arguments here to match your current code: (userId, categoryId)
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(existingCategory)).thenReturn(existingCategory);
        when(categoryMapper.toResponse(existingCategory)).thenReturn(expectedResponse);

        // 2. Act
        CategoryResponse actualResponse = categoryService.updateCategory(userId, categoryId, updateRequest);

        // 3. Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    @DisplayName("Should throw exception when updating a non-existent category")
    void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryUpdateRequest updateRequest = Instancio.create(CategoryUpdateRequest.class);

        // Note: Match the argument order with your service implementation
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.updateCategory(userId, categoryId, updateRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getErrorCode().getHttpStatus());
        assertEquals("Category Not Found", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }
    //</editor-fold>

    //<editor-fold desc="Test deactivateCategory">
    @Test
    @DisplayName("Should successfully deactivate a category")
    void deactivateCategory_ShouldSetActiveToFalse_WhenCategoryExists() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category existingCategory = Instancio.of(Category.class)
                .set(Select.field(Category::isActive), true) // Ensures it starts as true
                .create();

        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(existingCategory)).thenReturn(existingCategory);

        // 2. Act
        categoryService.deactivateCategory(userId, categoryId);

        // 3. Assert
        assertFalse(existingCategory.isActive(), "The category's active status should be false");
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    @DisplayName("Should throw exception when deactivating a non-existent category")
    void deactivateCategory_ShouldThrowException_WhenCategoryNotFound() {
        // 1. Arrange
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        // 2. Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> categoryService.deactivateCategory(userId, categoryId)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getErrorCode().getHttpStatus());
        verify(categoryRepository, never()).save(any(Category.class));
    }
    //</editor-fold>
}
