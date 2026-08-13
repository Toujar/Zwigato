package com.fooddelivery.service;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contract for food category operations.
 */
public interface CategoryService {

    List<CategoryResponse> getAllActiveCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    // ──────────────────────────────────────────────────────────
    // Admin Methods
    // ──────────────────────────────────────────────────────────

    /**
     * Get all categories for admin (including inactive ones).
     *
     * @param isActive filter by active status (null = all)
     * @param pageable pagination info
     * @return paginated categories
     */
    Page<CategoryResponse> getAllCategoriesForAdmin(Boolean isActive, Pageable pageable);

    /**
     * Deactivate a category (soft delete).
     */
    void deactivateCategory(Long id);

    /**
     * Activate a previously deactivated category.
     */
    CategoryResponse activateCategory(Long id);
}
