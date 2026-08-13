package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.entity.Category;
import com.fooddelivery.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin category management endpoints.
 *
 * All endpoints require ADMIN role.
 *
 * GET    /api/admin/categories        → list all categories (including inactive)
 * POST   /api/admin/categories        → create new category
 * PUT    /api/admin/categories/{id}   → update category
 * DELETE /api/admin/categories/{id}   → soft delete category
 * PUT    /api/admin/categories/{id}/activate → activate category
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "10. Admin Dashboard")
public class AdminCategoryController {

    private final CategoryService categoryService;

    // ── GET /api/admin/categories ─────────────────────────────
    /**
     * Get all categories (including inactive ones).
     * Admin can see soft-deleted categories.
     */
    @GetMapping
    @Operation(summary = "Get all categories for admin")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Boolean isActive) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryResponse> categories = categoryService.getAllCategoriesForAdmin(isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    // ── POST /api/admin/categories ────────────────────────────
    /**
     * Create a new category.
     */
    @PostMapping
    @Operation(summary = "Create new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        log.info("Admin created new category: {}", category.getName());
        return ResponseEntity.ok(ApiResponse.success(category, "Category created successfully"));
    }

    // ── PUT /api/admin/categories/{id} ────────────────────────
    /**
     * Update an existing category.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id, 
            @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        log.info("Admin updated category {}: {}", id, category.getName());
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    // ── DELETE /api/admin/categories/{id} ─────────────────────
    /**
     * Soft delete a category (mark as inactive).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate category")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        log.info("Admin deactivated category {}", id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deactivated successfully"));
    }

    // ── PUT /api/admin/categories/{id}/activate ──────────────
    /**
     * Activate a previously deactivated category.
     */
    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate category")
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(@PathVariable Long id) {
        CategoryResponse category = categoryService.activateCategory(id);
        log.info("Admin activated category {}: {}", id, category.getName());
        return ResponseEntity.ok(ApiResponse.success(category, "Category activated successfully"));
    }
}