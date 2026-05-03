package com.seap.smartfinancetracker.category.controller;

import com.seap.smartfinancetracker.category.dto.CategoryCreateRequest;
import com.seap.smartfinancetracker.category.dto.CategoryResponse;
import com.seap.smartfinancetracker.category.dto.CategoryUpdateRequest;
import com.seap.smartfinancetracker.category.service.CategoryService;
import com.seap.smartfinancetracker.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("categories")
@AllArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@CurrentUserId UUID userId
            , @Valid @RequestBody CategoryCreateRequest categoryCreateRequest) {
        CategoryResponse categoryResponse = categoryService.createCategory(userId, categoryCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryResponse);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@CurrentUserId UUID userId, @PathVariable UUID categoryId) {
        CategoryResponse categoryResponse = categoryService.getCategoryById(userId, categoryId);
        return ResponseEntity.ok().body(categoryResponse);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(@CurrentUserId UUID userId) {
        List<CategoryResponse> categoryResponses = categoryService.getAllCategoriesForUser(userId);

        return ResponseEntity.ok().body(categoryResponses);
    }

    @PutMapping("/{categoryId}")
    public  ResponseEntity<CategoryResponse> updateCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest categoryUpdateRequest) {
        CategoryResponse updatedCategoryResponse = categoryService.updateCategory(userId, categoryId, categoryUpdateRequest);
        return ResponseEntity.ok().body(updatedCategoryResponse);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivateCategory(
            @CurrentUserId UUID userId,
            @PathVariable UUID categoryId) {
        categoryService.deactivateCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
