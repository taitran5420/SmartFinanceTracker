package com.seap.smartfinancetracker.category.service;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.exception.CategoryErrorCode;
import com.seap.smartfinancetracker.category.mapper.CategoryMapper;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import com.seap.smartfinancetracker.common.exception.BusinessException;
import com.seap.smartfinancetracker.user.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of category service handling user categories.
 */
@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(UUID userId, CategoryCreateRequest categoryRequest) {
        Category category = categoryMapper.toEntity(userId, categoryRequest);
        Category createdCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(createdCategory);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Ensure Category belongs to user or is defaults
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID userId, UUID categoryId) {
        return categoryMapper.toResponse(getCategoryForReadOrThrow(userId, categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForUser(UUID userId) {
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        List<Category> defaultCategories = categoryRepository.findByUserIdIsNull();

        return Stream.concat(userCategories.stream(), defaultCategories.stream())
                .filter(Objects::nonNull)
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryUpdateRequest categoryUpdateRequest) {
        Category existingCategory = getOwnedCategoryOrThrow(userId, categoryId);

        existingCategory.setCategoryName(categoryUpdateRequest.categoryName());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deactivateCategory(UUID userId, UUID categoryId) {
        Category existingCategory = getOwnedCategoryOrThrow(userId, categoryId);

        existingCategory.setActive(false);
        categoryRepository.save(existingCategory);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b> Ensure Category belongs to user or is defaults
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Category getCategoryEntity(UUID userId, UUID categoryId) {
        return getCategoryForReadOrThrow(userId, categoryId);
    }

    /**
     * Use for read action
     * allow user to read default categories
     */
    private Category getCategoryForReadOrThrow(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        User categoryUser = category.getUser();
        boolean isDefault = categoryUser == null;
        boolean isOwner = !isDefault && categoryUser.getId().equals(userId);

        if (!isDefault && !isOwner) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_ACCESS_DENIED);
        }

        return category;
    }

    /**
     * Use for update / delete action
     * restrict user to update default categories
     */
    private Category getOwnedCategoryOrThrow(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }
}
