package com.fooddelivery.service.impl;

import com.fooddelivery.config.CacheConstants;
import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.entity.Category;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    /**
     * Cache the full active list under a fixed key "allActive".
     * The list rarely changes, so a 30-min TTL (set in RedisConfig) is fine.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CATEGORIES, key = "'allActive'")
    public List<CategoryResponse> getAllActiveCategories() {
        // ── CACHE MISS ── Only hits MySQL on the first call after startup
        // or after a create/update/delete evicts this key.
        log.info("[CACHE MISS] Loading all active categories from MySQL");
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cache individual categories by their ID.
     * Key: zwigato:categories::<id>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CATEGORIES, key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        // ── CACHE MISS ── Only prints when this category id is not in Redis.
        log.info("[CACHE MISS] Loading category from MySQL id={}", id);
        Category category = findById(id);
        return toResponse(category);
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    /**
     * After creating a new category the "allActive" list is stale.
     * @CacheEvict removes it so the next GET rebuilds from DB.
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConstants.CATEGORIES, key = "'allActive'")
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

    /**
     * @Caching groups two operations atomically:
     *   1. @CachePut  — writes the updated category back under its ID key
     *                   so a subsequent getCategoryById() hits cache, not DB.
     *   2. @CacheEvict — nukes the "allActive" list because the category
     *                   name/image may have changed.
     */
    @Override
    @Transactional
    @Caching(
        put    = { @CachePut(value = CacheConstants.CATEGORIES, key = "#id") },
        evict  = { @CacheEvict(value = CacheConstants.CATEGORIES, key = "'allActive'") }
    )
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

    /**
     * Soft-delete evicts BOTH:
     *   - The individual entry for this ID
     *   - The "allActive" list (category disappears from the list)
     */
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConstants.CATEGORIES, key = "#id"),
        @CacheEvict(value = CacheConstants.CATEGORIES, key = "'allActive'")
    })
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
