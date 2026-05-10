package com.seap.smartfinancetracker.category.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to update category, only category name is allowed to be updated.
 *
 * @param categoryName the update category's name
 */
public record CategoryUpdateRequest(
        @NotBlank(message = "Category name cannot be blank")
        String categoryName
) { }
