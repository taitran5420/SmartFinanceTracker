package com.seap.smartfinancetracker.category.service;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.entity.Category;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing user categories.
 *
 * <p>Categories are scoped by user, meaning each operation requires a valid userId
 * to ensure data isolation between users.
 *
 * <p>Supports CRUD-like operations:
 * <ul>
 *     <li>Create new category for a user</li>
 *     <li>Retrieve categories by user</li>
 *     <li>Update category owned by user</li>
 *     <li>Deactivate category instead of deleting</li>
 * </ul>
 */
public interface CategoryService {

    /**
     * Creates a new category for a specific user.
     *
     * @param userId ID of the user who owns the category
     * @param category request payload containing category information
     * @return created {@link CategoryResponse}
     */
    CategoryResponse createCategory(UUID userId, CategoryCreateRequest category);

    /**
     * Retrieves a category by its ID for a specific user.
     *
     * @param userId ID of the user who owns the category
     * @param categoryId ID of the category to retrieve
     * @return {@link CategoryResponse} if found and belongs to the user
     */
    CategoryResponse getCategoryById(UUID userId, UUID categoryId);

    /**
     * Retrieves all categories belonging to a specific user.
     *
     * @param userId ID of the user whose categories are being retrieved
     * @return list of {@link CategoryResponse} for the user
     */
    List<CategoryResponse> getAllCategoriesForUser(UUID userId);

    /**
     * Updates an existing category owned by a specific user.
     *
     * @param userId ID of the user who owns the category
     * @param categoryId ID of the category to update
     * @param category request payload containing updated category information
     * @return updated {@link CategoryResponse}
     */
    CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryUpdateRequest category);

    /**
     * Deactivates a category instead of permanently deleting it.
     *
     * <p>This performs a soft delete by marking the category as inactive.</p>
     *
     * @param userId ID of the user who owns the category
     * @param categoryId ID of the category to deactivate
     */
    void deactivateCategory(UUID userId, UUID categoryId);

    /**
     * INTERNAL USE ONLY: Retrieves the Category entity for other services.
     * Validates user ownership and active status.
     *
     * @param userId ID of the user who owns the category
     * @param categoryId ID of the category to retrieve
     * @return {@link com.seap.smartfinancetracker.category.entity.Category} entity
     * @throws com.seap.smartfinancetracker.common.exception.BusinessException if category not found or unauthorized
     */
    Category getCategoryEntity(UUID userId, UUID categoryId);
}
