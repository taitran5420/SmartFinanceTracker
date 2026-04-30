package com.seap.smartfinancetracker.category.service;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse createCategory(UUID userId, CategoryCreateRequest category);

    List<CategoryResponse> getAllCategoriesForUser(UUID userId);

    CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryUpdateRequest category);

    void deactivateCategory(UUID userId, UUID categoryId);
}
