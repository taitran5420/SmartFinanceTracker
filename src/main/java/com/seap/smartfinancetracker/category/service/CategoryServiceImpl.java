package com.seap.smartfinancetracker.category.service;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.category.mapper.CategoryMapper;
import com.seap.smartfinancetracker.category.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID userId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

        boolean isDefault = category.getUser() == null;
        boolean isOwner = !isDefault && category.getUser().getId().equals(userId);

        if (!isDefault && !isOwner) {
            throw new IllegalArgumentException("You do not have permission to access this category!");
        }

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForUser(UUID userId) {
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        List<Category> defaultCategories = categoryRepository.findByUserIdIsNull();

        return Stream.concat(userCategories.stream(), defaultCategories.stream())
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryUpdateRequest categoryUpdateRequest) {
        Category existingCategory = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

        existingCategory.setCategoryName(categoryUpdateRequest.categoryName());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deactivateCategory(UUID userId, UUID categoryId) {
        Category existingCategory = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

        existingCategory.setActive(false);
        categoryRepository.save(existingCategory);
    }
}
