package com.fooddelivery.service;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.CategoryResponse;

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
}
