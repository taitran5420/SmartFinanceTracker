package com.seap.smartfinancetracker.category.controller;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing user categories.
 * <p>
 * Provides endpoints for creating, retrieving, updating, and deactivating categories
 * within the finance tracker.
 * </p>
 */
@RestController
@RequestMapping("categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * Creates a new category for the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @param categoryCreateRequest the payload containing category creation data
     * @return a {@link ResponseEntity} containing the created {@link CategoryResponse}
     *         with HTTP status 201 (Created)
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@CurrentUserId UUID userId
            , @Valid @RequestBody CategoryCreateRequest categoryCreateRequest) {
        CategoryResponse categoryResponse = categoryService.createCategory(userId, categoryCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
    }

    /**
     * Retrieves a specific category by its unique identifier.
     *
     * @param userId the ID of the currently authenticated user
     * @param categoryId the unique identifier of the category to retrieve
     * @return a {@link ResponseEntity} containing the requested {@link CategoryResponse}
     *         with HTTP status 200 (OK)
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@CurrentUserId UUID userId, @PathVariable UUID categoryId) {
        CategoryResponse categoryResponse = categoryService.getCategoryById(userId, categoryId);
        return ResponseEntity.ok().body(categoryResponse);
    }

    /**
     * Retrieves all active categories belonging to the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @return a {@link ResponseEntity} containing a list of {@link CategoryResponse}
     *         with HTTP status 200 (OK)
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(@CurrentUserId UUID userId) {
        List<CategoryResponse> categoryResponses = categoryService.getAllCategoriesForUser(userId);

        return ResponseEntity.ok().body(categoryResponses);
    }

    /**
     * Updates an existing category for the authenticated user.
     *
     * @param userId the ID of the currently authenticated user
     * @param categoryId the unique identifier of the category to update
     * @param categoryUpdateRequest the payload containing the updated category data
     * @return a {@link ResponseEntity} containing the updated {@link CategoryResponse}
     *         with HTTP status 200 (OK)
     */
    @PutMapping("/{categoryId}")
    public  ResponseEntity<CategoryResponse> updateCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest categoryUpdateRequest) {
        CategoryResponse updatedCategoryResponse = categoryService.updateCategory(userId, categoryId, categoryUpdateRequest);
        return ResponseEntity.ok().body(updatedCategoryResponse);
    }

    /**
     * Deactivates (soft deletes) a specific category for the authenticated user.
     * <p>
     * Note: This operation performs a soft delete to maintain data integrity for
     * past transactions associated with this category.
     * </p>
     *
     * @param userId the ID of the currently authenticated user
     * @param categoryId the unique identifier of the category to deactivate
     * @return an empty {@link ResponseEntity} with HTTP status 204 (No Content)
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID categoryId) {
        categoryService.deactivateCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
