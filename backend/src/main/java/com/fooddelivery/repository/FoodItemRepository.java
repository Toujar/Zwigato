package com.fooddelivery.repository;

import com.fooddelivery.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 *  Repository : FoodItemRepository
 *  Entity     : FoodItem
 *  Table      : food_items
 * ============================================================
 *
 *  The menu layer of the system.  Every query is scoped either
 *  to a restaurant (public menu) or to a category (admin filter).
 *
 *  Custom methods cover:
 *   1. Public menu fetching (restaurant menu page)
 *   2. Category-scoped fetching (menu grouping by category)
 *   3. Keyword search within a restaurant's menu
 *   4. Dietary filtering (vegetarian items)
 *   5. Availability toggling (owner action)
 *   6. Price-range filtering (advanced search)
 *   7. Existence and ownership guards
 *   8. Counts and statistics
 * ============================================================
 */
@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    // ----------------------------------------------------------
    // 1. Full Menu Fetching
    // ----------------------------------------------------------

    /**
     * Returns all food items for a restaurant, regardless of availability.
     *
     * Used by: Restaurant owner's menu management page — shows all items
     * including unavailable ones so they can be toggled back on.
     *
     * SQL: SELECT * FROM food_items WHERE restaurant_id = ?
     */
    List<FoodItem> findByRestaurant_Id(Long restaurantId);

    /**
     * Returns only the AVAILABLE food items for a restaurant.
     *
     * Used by: Customer-facing restaurant detail / menu page.
     * Unavailable items are hidden from customers.
     *
     * SQL: SELECT * FROM food_items WHERE restaurant_id = ? AND is_available = true
     */
    List<FoodItem> findByRestaurant_IdAndIsAvailableTrue(Long restaurantId);

    /**
     * Returns available food items for a restaurant, fetching
     * the category in the same SQL JOIN to avoid N+1 queries
     * when rendering a grouped menu.
     *
     * Used by: Restaurant menu page where items are grouped by category.
     * JOIN FETCH loads category in one query instead of one per item.
     *
     * JPQL: SELECT f FROM FoodItem f
     *       JOIN FETCH f.category
     *       WHERE f.restaurant.id = :restaurantId AND f.isAvailable = true
     *       ORDER BY f.category.name, f.name
     */
    @Query("""
        SELECT f FROM FoodItem f
        JOIN FETCH f.category
        WHERE f.restaurant.id = :restaurantId
          AND f.isAvailable   = true
        ORDER BY f.category.name ASC, f.name ASC
        """)
    List<FoodItem> findAvailableWithCategory(@Param("restaurantId") Long restaurantId);

    /**
     * Finds a single food item by its ID, verifying it belongs to
     * the given restaurant.
     *
     * Used by: Authorization guard — before letting an owner update or
     * delete an item, confirm it belongs to their restaurant.
     *
     * SQL: SELECT * FROM food_items WHERE id = ? AND restaurant_id = ?
     */
    Optional<FoodItem> findByIdAndRestaurant_Id(Long id, Long restaurantId);

    // ----------------------------------------------------------
    // 2. Category-Scoped Fetching
    // ----------------------------------------------------------

    /**
     * Returns all food items in a given category, across all restaurants.
     *
     * Used by: Category browse page — "All Pizza items" regardless of restaurant.
     *
     * SQL: SELECT * FROM food_items WHERE category_id = ?
     */
    List<FoodItem> findByCategory_Id(Long categoryId);

    /**
     * Returns available items for a specific restaurant filtered by category.
     *
     * Used by: Restaurant menu page category tab — clicking "Burgers"
     * shows only burgers for that specific restaurant.
     *
     * SQL: SELECT * FROM food_items
     *      WHERE restaurant_id = ? AND category_id = ? AND is_available = true
     */
    List<FoodItem> findByRestaurant_IdAndCategory_IdAndIsAvailableTrue(
            Long restaurantId, Long categoryId);

    /**
     * Returns all items (including unavailable) for a restaurant + category.
     *
     * Used by: Owner's menu editor filtered by category tab.
     *
     * SQL: SELECT * FROM food_items WHERE restaurant_id = ? AND category_id = ?
     */
    List<FoodItem> findByRestaurant_IdAndCategory_Id(Long restaurantId, Long categoryId);

    // ----------------------------------------------------------
    // 3. Keyword Search Within a Restaurant
    // ----------------------------------------------------------

    /**
     * Case-insensitive keyword search within a single restaurant's menu.
     * Only searches available items (customer view).
     *
     * Used by: Search bar on the restaurant detail page
     * — "Find your favourites".
     *
     * @param restaurantId  restrict search to this restaurant
     * @param keyword       partial item name to search for
     * @param pageable      pagination and sort
     *
     * JPQL: WHERE restaurant_id = :restaurantId AND is_available = true
     *         AND LOWER(name) LIKE %keyword%
     */
    @Query("""
        SELECT f FROM FoodItem f
        WHERE f.restaurant.id = :restaurantId
          AND f.isAvailable   = true
          AND LOWER(f.name)   LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<FoodItem> searchAvailableByName(
            @Param("restaurantId") Long restaurantId,
            @Param("keyword")      String keyword,
            Pageable pageable);

    /**
     * Cross-restaurant keyword search (admin / discovery view).
     *
     * Used by: Platform-wide food search — customers typing "paneer"
     * see it from multiple restaurants.
     *
     * @param keyword  partial item name
     * @param pageable pagination and sort
     */
    @Query("""
        SELECT f FROM FoodItem f
        WHERE f.isAvailable = true
          AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<FoodItem> searchAllAvailableByName(
            @Param("keyword") String keyword,
            Pageable pageable);

    // ----------------------------------------------------------
    // 4. Dietary Filtering
    // ----------------------------------------------------------

    /**
     * Returns all available vegetarian items for a restaurant.
     *
     * Used by: "Veg Only" toggle filter on the restaurant menu page.
     *
     * SQL: SELECT * FROM food_items
     *      WHERE restaurant_id = ? AND is_vegetarian = true AND is_available = true
     */
    List<FoodItem> findByRestaurant_IdAndIsVegetarianTrueAndIsAvailableTrue(Long restaurantId);

    /**
     * Returns all available vegetarian items across all restaurants.
     *
     * Used by: Platform-wide "Veg Only" discovery filter.
     *
     * SQL: SELECT * FROM food_items WHERE is_vegetarian = true AND is_available = true
     */
    List<FoodItem> findByIsVegetarianTrueAndIsAvailableTrue();

    // ----------------------------------------------------------
    // 5. Availability Toggle (Owner Action)
    // ----------------------------------------------------------

    /**
     * Directly updates the is_available flag for a food item
     * without loading the full entity.
     *
     * Used by: Owner "Mark as available / unavailable" toggle on menu.
     * Efficient — only one column is updated.
     *
     * @Modifying   — required for JPQL UPDATE.
     * @Transactional — the update runs atomically.
     *
     * Returns: 1 if updated, 0 if item not found.
     */
    @Modifying
    @Transactional
    @Query("UPDATE FoodItem f SET f.isAvailable = :isAvailable WHERE f.id = :id")
    int updateAvailability(@Param("id") Long id, @Param("isAvailable") Boolean isAvailable);

    /**
     * Marks all food items of a restaurant as unavailable in one query.
     *
     * Used by: When a restaurant is closed or deactivated — all its
     * items should also become unavailable to prevent add-to-cart.
     *
     * Returns: number of rows updated.
     */
    @Modifying
    @Transactional
    @Query("UPDATE FoodItem f SET f.isAvailable = false WHERE f.restaurant.id = :restaurantId")
    int markAllUnavailableByRestaurant(@Param("restaurantId") Long restaurantId);

    // ----------------------------------------------------------
    // 6. Price-Range Filtering
    // ----------------------------------------------------------

    /**
     * Returns available items for a restaurant within a price range.
     *
     * Used by: Advanced menu filter — customers can filter
     * items between ₹100 and ₹300 for example.
     *
     * @param restaurantId  the target restaurant
     * @param minPrice      lower price bound (inclusive)
     * @param maxPrice      upper price bound (inclusive)
     * @param pageable      pagination and sort
     *
     * JPQL: WHERE restaurant_id = ? AND is_available = true
     *         AND price BETWEEN :min AND :max
     */
    @Query("""
        SELECT f FROM FoodItem f
        WHERE f.restaurant.id = :restaurantId
          AND f.isAvailable   = true
          AND f.price         BETWEEN :minPrice AND :maxPrice
        """)
    Page<FoodItem> findByRestaurantAndPriceRange(
            @Param("restaurantId") Long restaurantId,
            @Param("minPrice")     BigDecimal minPrice,
            @Param("maxPrice")     BigDecimal maxPrice,
            Pageable pageable);

    // ----------------------------------------------------------
    // 7. Existence and Ownership Guards
    // ----------------------------------------------------------

    /**
     * Checks if a food item exists AND belongs to the given restaurant.
     *
     * Used by: CartService.addItem() — validates that the item
     * being added actually belongs to the restaurant the cart is
     * locked to, preventing cross-restaurant cart corruption.
     *
     * SQL: SELECT COUNT(*) > 0 FROM food_items WHERE id = ? AND restaurant_id = ?
     */
    boolean existsByIdAndRestaurant_Id(Long id, Long restaurantId);

    /**
     * Checks if a food item exists, is available, and belongs to a restaurant.
     *
     * Used by: CartService.addItem() — the strictest guard.
     * Rejects adding unavailable items to the cart.
     *
     * SQL: SELECT COUNT(*) > 0 FROM food_items
     *      WHERE id = ? AND restaurant_id = ? AND is_available = true
     */
    boolean existsByIdAndRestaurant_IdAndIsAvailableTrue(Long id, Long restaurantId);

    // ----------------------------------------------------------
    // 8. Counts and Statistics
    // ----------------------------------------------------------

    /**
     * Counts all food items for a restaurant.
     *
     * Used by: Owner dashboard — "Your menu has 34 items".
     *
     * SQL: SELECT COUNT(*) FROM food_items WHERE restaurant_id = ?
     */
    long countByRestaurant_Id(Long restaurantId);

    /**
     * Counts available food items for a restaurant.
     *
     * Used by: Owner dashboard — "18 items currently available".
     *
     * SQL: SELECT COUNT(*) FROM food_items WHERE restaurant_id = ? AND is_available = true
     */
    long countByRestaurant_IdAndIsAvailableTrue(Long restaurantId);
}
