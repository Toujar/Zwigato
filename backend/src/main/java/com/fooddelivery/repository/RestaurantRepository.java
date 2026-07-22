package com.fooddelivery.repository;

import com.fooddelivery.entity.Restaurant;
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
 *  Repository : RestaurantRepository
 *  Entity     : Restaurant
 *  Table      : restaurants
 * ============================================================
 *
 *  The most query-heavy repository in the system.
 *  Customers browse restaurants by city, name, and open status.
 *  Owners manage their own restaurants.
 *  Admin manages all restaurants.
 *
 *  Custom methods cover:
 *   1. Customer-facing browse (active + open + city + keyword)
 *   2. Owner-facing management (find own restaurants)
 *   3. Admin management (all, soft-delete toggle)
 *   4. Duplicate prevention
 *   5. Rating updates (async, targeted UPDATE — no full load)
 *   6. Counts and statistics
 * ============================================================
 */
@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    // ----------------------------------------------------------
    // 1. Customer-Facing Browse Queries
    // ----------------------------------------------------------

    /**
     * Returns a paginated list of restaurants that are both active
     * (not soft-deleted) AND currently open for orders.
     *
     * Used by: Home page — the default restaurant grid shown to customers.
     * Paginated to avoid loading hundreds of restaurants at once.
     *
     * SQL: SELECT * FROM restaurants WHERE is_active = true AND is_open = true
     *      ORDER BY ... LIMIT ? OFFSET ?
     */
    Page<Restaurant> findByIsActiveTrueAndIsOpenTrue(Pageable pageable);

    /**
     * Returns all active (non-soft-deleted) restaurants in a given city,
     * case-insensitively.
     *
     * Used by: City-based filtering on the restaurant browse page.
     * MySQL's LOWER() is applied by Spring Data's IgnoreCase suffix.
     *
     * SQL: SELECT * FROM restaurants
     *      WHERE is_active = true AND LOWER(city) = LOWER(?)
     */
    List<Restaurant> findByIsActiveTrueAndCityIgnoreCase(String city);

    /**
     * Returns a paginated list of active + open restaurants in a given city.
     *
     * Used by: Home page when a city filter chip is selected.
     * Combines city filter, open status, and pagination in one query.
     *
     * SQL: SELECT * FROM restaurants
     *      WHERE is_active = true AND is_open = true AND LOWER(city) = LOWER(?)
     */
    Page<Restaurant> findByIsActiveTrueAndIsOpenTrueAndCityIgnoreCase(
            String city, Pageable pageable);

    /**
     * Keyword search across restaurant name and city.
     *
     * Used by: Search bar on the home page.
     * Case-insensitive LIKE match against both name and city columns.
     * Only active restaurants are included.
     * Paginated for large result sets.
     *
     * @param keyword  partial name or city to search for
     * @param pageable pagination and sort
     *
     * JPQL: WHERE is_active = true
     *         AND (LOWER(name) LIKE %keyword% OR LOWER(city) LIKE %keyword%)
     */
    @Query("""
        SELECT r FROM Restaurant r
        WHERE r.isActive = true
          AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR  LOWER(r.city) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Restaurant> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Keyword search scoped to a specific city.
     *
     * Used by: Search bar when a city filter is already active.
     * Narrows results to the selected city AND matches the keyword.
     *
     * @param keyword  partial name to search for
     * @param city     city to restrict search to
     * @param pageable pagination and sort
     */
    @Query("""
        SELECT r FROM Restaurant r
        WHERE r.isActive = true
          AND LOWER(r.city) = LOWER(:city)
          AND LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<Restaurant> searchByKeywordAndCity(
            @Param("keyword") String keyword,
            @Param("city")    String city,
            Pageable pageable);

    /**
     * Returns the top-N restaurants by rating in a given city.
     *
     * Used by: "Top Restaurants Near You" section on the home page.
     * Sorted by rating descending; pageable limits result count (e.g., top 10).
     *
     * SQL: SELECT * FROM restaurants
     *      WHERE is_active = true AND is_open = true AND LOWER(city) = LOWER(?)
     *      ORDER BY rating DESC LIMIT ?
     */
    Page<Restaurant> findByIsActiveTrueAndIsOpenTrueAndCityIgnoreCaseOrderByRatingDesc(
            String city, Pageable pageable);

    // ----------------------------------------------------------
    // 2. Owner-Facing Queries
    // ----------------------------------------------------------

    /**
     * Returns all restaurants owned by a specific user.
     *
     * Used by: RestaurantOwner dashboard — "My Restaurants" list.
     * A single owner may run multiple branches.
     * Includes soft-deleted restaurants so the owner can see
     * deactivated branches (Admin action).
     *
     * SQL: SELECT * FROM restaurants WHERE owner_id = ?
     */
    List<Restaurant> findByOwner_Id(Long ownerId);

    /**
     * Returns all ACTIVE restaurants owned by a specific user.
     *
     * Used by: Owner's "Add food item" flow — only shows their active
     * restaurants in the restaurant picker dropdown.
     *
     * SQL: SELECT * FROM restaurants WHERE owner_id = ? AND is_active = true
     */
    List<Restaurant> findByOwner_IdAndIsActiveTrue(Long ownerId);

    /**
     * Finds a specific restaurant by its ID AND verifying it belongs
     * to the given owner.
     *
     * Used by: Authorization guard — before letting an owner update or
     * delete a restaurant, confirm they actually own it.
     *
     * SQL: SELECT * FROM restaurants WHERE id = ? AND owner_id = ?
     */
    Optional<Restaurant> findByIdAndOwner_Id(Long id, Long ownerId);

    // ----------------------------------------------------------
    // 3. Admin Management Queries
    // ----------------------------------------------------------

    /**
     * Returns all active restaurants as a non-paginated list.
     *
     * Used by: Admin bulk operations or reports where full list is needed.
     * For large datasets, prefer the paginated variant.
     *
     * SQL: SELECT * FROM restaurants WHERE is_active = true
     */
    List<Restaurant> findByIsActiveTrue();

    // ----------------------------------------------------------
    // 4. Duplicate Prevention
    // ----------------------------------------------------------

    /**
     * Checks whether a restaurant with the same name already exists
     * in the same city (case-insensitive on both fields).
     *
     * Used by: RestaurantService.createRestaurant() to prevent
     * registering "Pizza Hut" twice in "Mumbai".
     *
     * SQL: SELECT COUNT(*) > 0 FROM restaurants
     *      WHERE LOWER(name) = LOWER(?) AND LOWER(city) = LOWER(?)
     */
    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    // ----------------------------------------------------------
    // 5. Targeted UPDATE Queries (no full entity load)
    // ----------------------------------------------------------

    /**
     * Toggles the isOpen flag without loading the full Restaurant entity.
     *
     * Used by: Owner "Open / Close Restaurant" toggle on their dashboard.
     * Much cheaper than load → set → save for a single boolean column.
     *
     * @Modifying   — required for JPQL UPDATE statements.
     * @Transactional — the update runs inside a transaction.
     *
     * JPQL: UPDATE restaurants SET is_open = :isOpen WHERE id = :id
     * Returns: number of rows updated (1 = success, 0 = not found)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Restaurant r SET r.isOpen = :isOpen WHERE r.id = :id")
    int updateIsOpen(@Param("id") Long id, @Param("isOpen") Boolean isOpen);

    /**
     * Soft-deletes (or restores) a restaurant without loading the entity.
     *
     * Used by: Admin "Activate / Deactivate Restaurant" action.
     *
     * JPQL: UPDATE restaurants SET is_active = :isActive WHERE id = :id
     */
    @Modifying
    @Transactional
    @Query("UPDATE Restaurant r SET r.isActive = :isActive WHERE r.id = :id")
    int updateIsActive(@Param("id") Long id, @Param("isActive") Boolean isActive);

    /**
     * Updates the average rating of a restaurant directly in the DB.
     *
     * Used by: An async rating-aggregation job that recalculates the
     * average from all order reviews. Avoids loading the full entity
     * just to update one decimal column.
     *
     * @param id     the restaurant to update
     * @param rating the newly computed average rating
     *
     * JPQL: UPDATE restaurants SET rating = :rating WHERE id = :id
     */
    @Modifying
    @Transactional
    @Query("UPDATE Restaurant r SET r.rating = :rating WHERE r.id = :id")
    int updateRating(@Param("id") Long id, @Param("rating") BigDecimal rating);

    // ----------------------------------------------------------
    // 6. Counts and Statistics
    // ----------------------------------------------------------

    /**
     * Counts all active restaurants.
     *
     * Used by: Admin dashboard stat card — "Total active restaurants: 48".
     *
     * SQL: SELECT COUNT(*) FROM restaurants WHERE is_active = true
     */
    long countByIsActiveTrue();

    /**
     * Counts active + open restaurants in a specific city.
     *
     * Used by: City landing page subtitle — "23 restaurants open near you".
     *
     * SQL: SELECT COUNT(*) FROM restaurants
     *      WHERE is_active = true AND is_open = true AND LOWER(city) = LOWER(?)
     */
    long countByIsActiveTrueAndIsOpenTrueAndCityIgnoreCase(String city);

    /**
     * Returns a distinct list of cities that have at least one active restaurant.
     *
     * Used by: City picker dropdown — auto-populated from real data rather
     * than a hardcoded list.
     *
     * JPQL: SELECT DISTINCT r.city FROM Restaurant r WHERE r.isActive = true ORDER BY r.city
     */
    @Query("SELECT DISTINCT r.city FROM Restaurant r WHERE r.isActive = true ORDER BY r.city ASC")
    List<String> findDistinctActiveCities();
}
