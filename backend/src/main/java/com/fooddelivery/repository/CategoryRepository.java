package com.fooddelivery.repository;

import com.fooddelivery.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : CategoryRepository
 *  Entity     : Category
 *  Table      : categories
 * ============================================================
 *
 *  Categories are shared across all restaurants and managed by
 *  ADMIN users.  The query set is intentionally small — this is
 *  a reference/lookup table, not a high-write domain.
 *
 *  Custom methods cover:
 *   1. Case-insensitive name lookups (duplicate prevention + search)
 *   2. Active/inactive filtering for customer-facing vs admin views
 *   3. Existence check before INSERT
 *   4. Soft-delete toggle
 *   5. Counts used by admin dashboards
 * ============================================================
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ----------------------------------------------------------
    // 1. Name Lookups
    // ----------------------------------------------------------

    /**
     * Finds a category by its name, case-insensitively.
     *
     * Used by:
     *   - CategoryService.createCategory() to prevent inserting a duplicate
     *     when the admin types "pizza" vs "Pizza" vs "PIZZA".
     *   - Admin "find by name" search.
     *
     * SQL: SELECT * FROM categories WHERE LOWER(name) = LOWER(?)
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Checks whether a category with the given name already exists
     * (case-insensitive) WITHOUT loading the full entity.
     *
     * Used by: CategoryService.createCategory() guard — cheaper than
     * findByNameIgnoreCase() because it returns a boolean only.
     *
     * SQL: SELECT COUNT(*) > 0 FROM categories WHERE LOWER(name) = LOWER(?)
     */
    boolean existsByNameIgnoreCase(String name);

    // ----------------------------------------------------------
    // 2. Active / Inactive Filtering
    // ----------------------------------------------------------

    /**
     * Returns all categories marked as active.
     *
     * Used by:
     *   - Customer-facing menu page category filter chips.
     *   - FoodItem create/update form — only active categories shown.
     * Sorted by name for consistent UI ordering.
     *
     * SQL: SELECT * FROM categories WHERE is_active = true ORDER BY name ASC
     */
    List<Category> findByIsActiveTrueOrderByNameAsc();

    /**
     * Returns ALL categories regardless of active status.
     *
     * Used by: Admin category management page (shows both active
     * and deactivated entries so admins can reactivate them).
     *
     * SQL: SELECT * FROM categories ORDER BY name ASC
     */
    List<Category> findAllByOrderByNameAsc();

    // ----------------------------------------------------------
    // 3. Soft-Delete Toggle
    // ----------------------------------------------------------

    /**
     * Toggles is_active for a category without loading the full entity.
     *
     * Used by: Admin "Enable / Disable" action.
     * @Modifying is required for all JPQL UPDATE statements.
     * @Transactional keeps the update atomic.
     *
     * JPQL: UPDATE categories SET is_active = :isActive WHERE id = :id
     * Returns the number of rows updated (0 = not found, 1 = success).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Category c SET c.isActive = :isActive WHERE c.id = :id")
    int updateIsActive(@Param("id") Long id, @Param("isActive") Boolean isActive);

    // ----------------------------------------------------------
    // 4. Counts (Admin dashboard)
    // ----------------------------------------------------------

    /**
     * Counts how many active categories exist.
     *
     * Used by: Admin dashboard stat card — "Active categories: 12".
     *
     * SQL: SELECT COUNT(*) FROM categories WHERE is_active = true
     */
    long countByIsActiveTrue();

    // ----------------------------------------------------------
    // 5. Existence check by ID + isActive (guard for FoodItem creation)
    // ----------------------------------------------------------

    /**
     * Returns true if a category with the given ID exists AND is active.
     *
     * Used by: FoodItemService.createFoodItem() to reject attaching
     * a food item to a deactivated category before the INSERT.
     *
     * SQL: SELECT COUNT(*) > 0 FROM categories WHERE id = ? AND is_active = true
     */
    boolean existsByIdAndIsActiveTrue(Long id);

    // ----------------------------------------------------------
    // 6. Fetch with item count (Admin overview)
    // ----------------------------------------------------------

    /**
     * Returns each category paired with the count of food items
     * that reference it.
     *
     * Used by: Admin category list — shows "Pizza (42 items)" so the
     * admin knows whether it is safe to deactivate a category.
     *
     * Returns Object[] rows: [Category, Long itemCount]
     * The service layer should project this into a CategoryResponse DTO.
     *
     * JPQL: SELECT c, COUNT(f) FROM Category c LEFT JOIN c.foodItems f GROUP BY c
     */
    @Query("SELECT c, COUNT(f) FROM Category c LEFT JOIN c.foodItems f GROUP BY c ORDER BY c.name ASC")
    List<Object[]> findAllWithFoodItemCount();
}
