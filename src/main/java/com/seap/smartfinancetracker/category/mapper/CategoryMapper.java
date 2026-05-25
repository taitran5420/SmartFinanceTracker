package com.seap.smartfinancetracker.category.mapper;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.entity.Category;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper class responsible for converting between Category-related DTOs and Entity.
 *
 * <p>This class handles:
 * <ul>
 *     <li>Mapping {@link CategoryCreateRequest} to {@link Category}</li>
 *     <li>Mapping {@link Category} to {@link CategoryResponse}</li>
 * </ul>
 *
 * <p>Note: Newly created categories are always initialized with {@code active = true}.
 */
@Component
public class CategoryMapper {

    /**
     * Converts a category creation request into a {@link Category} entity.
     *
     * @param userId          the unique identifier of the user creating the category
     * @param categoryRequest the payload containing category details
     * @return a new {@link Category} entity ready to be persisted, or {@code null} if the request is null
     */
    public Category toEntity(UUID userId, CategoryCreateRequest categoryRequest) {
        if  (categoryRequest == null) {
            return null;
        }
        return Category.builder()
                .user(User.builder().id(userId).build())
                .categoryName(categoryRequest.categoryName())
                .transactionType(categoryRequest.transactionType())
                .active(true)
                .build();
    }

    /**
     * Converts a persisted {@link Category} entity into a {@link CategoryResponse} DTO.
     *
     * @param category the category entity retrieved from the database
     * @return the mapped response DTO, or {@code null} if the entity is null
     */
    public CategoryResponse toResponse(Category category) {
        if  (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .transactionType(category.getTransactionType())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .active(category.isActive())
                .build();
    }
}
