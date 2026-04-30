package com.seap.smartfinancetracker.category.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record CategoryUpdateRequest(
        @NotBlank(message = "Category name cannot be blank")
        String categoryName
) implements Serializable { }
