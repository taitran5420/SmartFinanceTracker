package com.seap.smartfinancetracker.category.dto;

import com.seap.smartfinancetracker.category.constant.CategoryValidationMessage;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to update category, only category name is allowed to be updated.
 *
 * @param categoryName the update category's name
 */
public record CategoryUpdateRequest(
        @NotBlank(message = CategoryValidationMessage.CATEGORY_NAME_CANNOT_BE_BLANK)
        String categoryName
) { }
