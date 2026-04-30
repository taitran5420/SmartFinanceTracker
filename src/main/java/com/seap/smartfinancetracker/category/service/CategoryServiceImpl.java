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
    public List<CategoryResponse> getAllCategoriesForUser(UUID userId) {
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        List<Category> defaultCategories = categoryRepository.findByUserIdIsNull();

        userCategories.addAll(defaultCategories);

        return userCategories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryUpdateRequest categoryUpdateRequest) {
        Category existingCategory = categoryRepository.findByIdAndUserId(userId, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category Not Found!"));

        existingCategory.setCategoryName(categoryUpdateRequest.categoryName());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void deactivateCategory(UUID userId, UUID categoryId) {
        // TODO
    }
}
