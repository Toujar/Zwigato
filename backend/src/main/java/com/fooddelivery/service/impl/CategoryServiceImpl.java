package com.fooddelivery.service.impl;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.entity.Category;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages food categories — a shared reference table for the whole platform.
 *
 * Key decisions:
 *  - deleteCategory() is a soft-delete. Hard deletion would break
 *    FoodItem rows that reference the category (ON DELETE RESTRICT).
 *  - All name comparisons are case-insensitive to prevent "pizza"
 *    and "Pizza" from existing as separate categories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = findById(id);
        return toResponse(category);
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "A category named '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} ({})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findById(id);

        // Only check for name conflict if the name is actually changing
        boolean nameChanged = !category.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "A category named '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        Category updated = categoryRepository.save(category);
        log.info("Category updated: {} ({})", updated.getName(), updated.getId());
        return toResponse(updated);
    }

    // ---------------------------------------------------------------
    // Delete (soft)
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);

        if (!category.getIsActive()) {
            throw new BadRequestException("Category is already deactivated");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Category deactivated: {} ({})", category.getName(), id);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
