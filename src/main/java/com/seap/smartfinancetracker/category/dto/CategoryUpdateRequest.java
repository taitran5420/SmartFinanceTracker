package com.seap.smartfinancetracker.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(
        @NotBlank(message = "Category name cannot be blank")
        String categoryName
) { }
