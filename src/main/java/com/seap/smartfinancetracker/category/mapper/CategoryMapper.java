package com.seap.smartfinancetracker.category.mapper;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.entity.Category;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CategoryMapper {
    public Category toEntity(UUID userId, CategoryCreateRequest categoryRequest) {
        if  (categoryRequest == null) {
            return null;
        }
        return Category.builder()
                .id(userId)
                .categoryName(categoryRequest.categoryName())
                .transactionType(categoryRequest.transactionType())
                .active(true)
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        if  (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .transactionType(category.getTransactionType())
                .active(category.isActive())
                .build();
    }
}
